#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Sat Jan  3 14:06:09 2026

@author: asheinen
"""

import pyomo.environ as pyo
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import matplotlib.dates as mdates

# Requirements: pip install pyomo highspy 
# highspy is the Python version of the HiGHS optimizer which is also used 
# in SciPy and in PyPSA. You could also use different solvers. 


def run_battery_optimization(price_series, cyclic=True, initial_soc_fraction=0.0): 
    """
    Optimizes battery dispatch based on a provided price time series.
    
    :param price_series: Pandas Series with DatetimeIndex (frequency determines dt)
    :param cyclic: If True, enforce SoC(end) == SoC(start) (default: True)
    :param initial_soc_fraction: Initial SoC as fraction of capacity (0.0 to 1.0)
    
    Notes: 
    The initial state of charge as defined in soc_initial is defined 
    for the time step ahead of the first charging or discharging. 
    If we have a time series for 00:00 to 23:45, then it's important to know
    this time series is defined for the start of a 15 min time block. As such 
    00:00 implies 00:00-00:15 and 23:45 implies 23:45-00:00 etc. 
    If we plot the SoC for the time step 0, then it will already reflect the 
    charging or discharging in the first 15 mins.
    Additionally, initial_soc_fraction=0.0 is just a default if you forget to 
    specify a value.
    
    """
    # 1. DYNAMIC INPUT HANDLING
    periods = len(price_series)
    time_index = price_series.index
    
    if periods < 2:
        raise ValueError("Price series must have at least 2 time steps")
    
    # Calculate dt (time step in hours)
    if hasattr(time_index, 'freq') and time_index.freq is not None:
        # For fixed frequency
        dt = time_index.freq.nanos / 1e9 / 3600
    else:
        # For irregular or inferred frequency, use the first interval
        dt = (time_index[1] - time_index[0]).total_seconds() / 3600
    
    if dt <= 0:
        raise ValueError(f"Time step dt must be positive, got {dt}")
    
    # 2. PARAMETER DEFINITION 
    # You could include it as a function argument and provide it externally
    cap_mwh     = 2.0          # Battery energy capacity (MWh)
    power_mw    = 1.0          # Maximum charging/discharging power (MW)
    eta         = 0.95         # Round-trip efficiency (applied at charging)
    
    if not (0.0 <= initial_soc_fraction <= 1.0):
        raise ValueError("initial_soc_fraction must be between 0 and 1")
    
    soc_initial = cap_mwh * initial_soc_fraction
    prices = price_series.values
    
    # 3. MODEL SETUP
    model = pyo.ConcreteModel(name="Battery_Arbitrage")
    model.T = pyo.RangeSet(0, periods - 1)
    
    # Decision Variables
    model.p_charge = pyo.Var(model.T, domain=pyo.NonNegativeReals, bounds=(0, power_mw))
    model.p_discharge = pyo.Var(model.T, domain=pyo.NonNegativeReals, bounds=(0, power_mw))
    model.soc = pyo.Var(model.T, domain=pyo.NonNegativeReals, bounds=(0, cap_mwh))
    
    # 4. OBJECTIVE FUNCTION
    def objective_rule(model):
        # Revenue from discharge minus cost of charging (at grid connection point)
        return sum((model.p_discharge[t] - model.p_charge[t]) * prices[t] * dt for t in model.T)
    
    model.obj = pyo.Objective(rule=objective_rule, sense=pyo.maximize)
    
    # 5. CONSTRAINTS
    
    # SoC Balance
    def soc_balance_rule(model, t):
        if t == 0:
            # Start with initial SoC
            return model.soc[t] == soc_initial + (model.p_charge[t] * eta - model.p_discharge[t] / eta) * dt
        return model.soc[t] == model.soc[t-1] + (model.p_charge[t] * eta - model.p_discharge[t] / eta) * dt
    
    model.soc_con = pyo.Constraint(model.T, rule=soc_balance_rule)
    
    # Cyclic Constraint (optional but recommended)
    if cyclic:
        def cyclic_soc_rule(model):
            # End SoC should equal initial SoC
            return model.soc[periods - 1] == soc_initial
        
        model.cyclic_soc_con = pyo.Constraint(rule=cyclic_soc_rule)
    
    # 6. SOLVER CALL (HiGHS)
    solver = pyo.SolverFactory('appsi_highs')
    result = solver.solve(model, load_solutions=True)
    
    # Check solver status
    if result.solver.termination_condition != pyo.TerminationCondition.optimal:
        print(f"Warning: Solver did not find optimal solution. Status: {result.solver.termination_condition}")
    
    # 7. DATA EXTRACTION
    output = []
    for t in model.T:
        output.append({
            'Timestamp': time_index[t],
            'Price_EUR_MWh': round(prices[t], 2),
            'Charge_MW': round(pyo.value(model.p_charge[t]), 3),
            'Discharge_MW': round(pyo.value(model.p_discharge[t]), 3),
            'SoC_MWh': round(pyo.value(model.soc[t]), 3)
        })
    
    df_results = pd.DataFrame(output).set_index('Timestamp')
    
    return df_results, pyo.value(model.obj), cap_mwh, power_mw


"""
    Example usage of the optimization function

"""

if __name__ == "__main__":
    
    """
        Some dummy data for testing, typical GenAI suggestion using random numbers along a shape
    
    """
    idx = pd.date_range("2026-01-01 00:00", periods=96, freq="15min") # Add a start
    periods = 96
    dummy_prices = pd.Series(
        50 - 20 * np.sin(np.linspace(0, 2 * np.pi, periods)) 
        + np.random.normal(0, 3, periods), 
        index=idx
    )

    """
        Importing real data: one day with quarter-hourly prices
    
    """
    file_location = "20260104_EPEX_SPOT_SDAC_IDA1.xlsx"
    df_prices = pd.read_excel(file_location,sheet_name='DE-LU', header=0, index_col='DateTime', parse_dates=['DateTime'])
    prices_day_ahead = pd.Series(df_prices['SDAC'],index=df_prices.index)
    prices_intraday  = pd.Series(df_prices['IDA1'],index=df_prices.index)
    
    # Running the optimization with a certain % SoC at the beginning (and end)
    soc_init = 0.5          # % state of charge at the beginning (and end)
    # replace the first argument in run_battery_optimization with the prices you want:
    # dummy_prices, prices_day_ahead, prices_intraday, ...
    df, total_profit, cap_mwh, power_mw = run_battery_optimization(prices_intraday, cyclic=True, initial_soc_fraction=soc_init)
    
    print(f"Total Profit: {total_profit:.2f} EUR")
    print(df.head(10))
    print("\nFinal SoC:", df['SoC_MWh'].iloc[-1], "MWh")

  
    """
        Some Plots, directly with a subplot to use a combined x-axis (time)
    
    """
    fig, axes = plt.subplots(3, 1, figsize=(14, 12), sharex=True) # 3 Subplots, shared axis
    plt.rcParams.update({'font.size': 14}) # font size

    # Plot 1: Prices
    axes[0].plot(df.index, df['Price_EUR_MWh'], color='blue', label='Price (€/MWh)')
    axes[0].set_title('Battery optimization results (price)')
    axes[0].set_ylabel('Price (€/MWh)')
    axes[0].grid(True, linestyle=':', alpha=0.7)
    axes[0].legend()

    # Plot 2: Charge / Discharge Power
    # Negative for discharging to how below the zero line
    axes[1].plot(df.index, df['Charge_MW'], color='green', label='Charge (MW)')
    axes[1].plot(df.index, -df['Discharge_MW'], color='red', label='Discharge (MW)') 
    axes[1].set_ylabel('Power (MW)')
    axes[1].axhline(0, color='grey', linestyle='--', linewidth=0.8) # Zero line for power 
    axes[1].fill_between(df.index, 0, df['Charge_MW'], color='green', alpha=0.2)
    axes[1].fill_between(df.index, 0, -df['Discharge_MW'], color='red', alpha=0.2)
    axes[1].set_ylim(-power_mw * 1.1, power_mw * 1.2) # y-axis linked to power
    axes[1].grid(True, linestyle=':', alpha=0.7)
    axes[1].legend()

    # Plot 3: State of Charge (SoC)
    axes[2].plot(df.index, df['SoC_MWh'], color='purple', label='SoC (MWh)')
    axes[2].set_ylabel('SoC (MWh)')
    axes[2].set_ylim(0, cap_mwh * 1.1) # y-axis linked to storage volume
    axes[2].axhline(cap_mwh, color='grey', linestyle='--', label='Max. Capacity')
    axes[2].fill_between(df.index, 0, df['SoC_MWh'], color='purple', alpha=0.2)
    axes[2].grid(True, linestyle=':', alpha=0.7)
    axes[2].legend()

    # Format axis
    formatter = mdates.DateFormatter('%H:%M')
    axes[2].xaxis.set_major_formatter(formatter)
    plt.xlabel('Time')
    plt.tight_layout() 
    plt.show()