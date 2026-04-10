#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Tue Jan 13 16:48:29 2026

@author: asheinen
"""

import pyomo.environ as pyo
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import matplotlib.dates as mdates

def run_load_shifting_optimization(load_series, cyclic=True):
    """
    Optimizes battery dispatch based on a provided load profile. 
  
    The objective is to minimize peak loads over the period. 
    
    Note: there is perfect foresight in the optimization approach over the 
    period. I.e. the input load_series is either understood as a forecast load.
    Or you need to call the function in smaller increments avoiding a 
    rigorous optimization over long periods.
    
    """
    
    periods = len(load_series)
    time_index = load_series.index
    dt = (time_index[1] - time_index[0]).total_seconds() / 3600
    
    # PARAMETER DEFINITION 
    # You could include it as a function argument and provide it externally
    cap_mwh     = 4.0           # Battery energy capacity (MWh)
    power_mw    = 1.0           # Maximum charging/discharging power (MW)
    eta         = 0.95          # Round-trip efficiency (applied at charging)
    
    soc_initial = cap_mwh * 0.05 # just setting a small minimum SoC
    
    customer_load = load_series.values

    model = pyo.ConcreteModel()
    model.T = pyo.RangeSet(0, periods - 1)

    model.p_charge = pyo.Var(model.T, domain=pyo.NonNegativeReals, bounds=(0, power_mw))
    model.p_discharge = pyo.Var(model.T, domain=pyo.NonNegativeReals, bounds=(0, power_mw))
    model.soc = pyo.Var(model.T, domain=pyo.NonNegativeReals, bounds=(0, cap_mwh))
    model.peak_load = pyo.Var(domain=pyo.NonNegativeReals)

    # OBJECTIVE FUNCTION
    # Minimize peak + tiny penalty for battery wear (avoids erratic cycling)
    epsilon = 0.00001
    model.obj = pyo.Objective(
        expr= model.peak_load + sum((model.p_charge[t] + model.p_discharge[t]) * epsilon for t in model.T),
        sense=pyo.minimize
    )

    def peak_con(model, t):
        return model.peak_load >= customer_load[t] + model.p_charge[t] - model.p_discharge[t]
    model.peak_con = pyo.Constraint(model.T, rule=peak_con)

    def soc_balance(model, t):
        if t == 0:
            return model.soc[t] == soc_initial + (model.p_charge[t] * eta - model.p_discharge[t] / eta) * dt
        return model.soc[t] == model.soc[t-1] + (model.p_charge[t] * eta - model.p_discharge[t] / eta) * dt
    model.soc_con = pyo.Constraint(model.T, rule=soc_balance)

    if cyclic:
        model.cyclic_con = pyo.Constraint(expr=model.soc[periods-1] == soc_initial)

    solver = pyo.SolverFactory('appsi_highs')
    solver.solve(model)

    output = []
    for t in model.T:
        output.append({
            'Timestamp': time_index[t],
            'Customer_Load_MW': customer_load[t],
            'Net_Grid_Load_MW': customer_load[t] + pyo.value(model.p_charge[t]) - pyo.value(model.p_discharge[t]),
            'Charge_MW': pyo.value(model.p_charge[t]),
            'Discharge_MW': pyo.value(model.p_discharge[t]),
            'SoC_MWh': pyo.value(model.soc[t])
        })
    return pd.DataFrame(output).set_index('Timestamp'), cap_mwh, power_mw

if __name__ == "__main__":
    
    # Generate Dummy Load (profile with two peaks)
    idx = pd.date_range("2026-01-13 00:00", periods=96, freq="15min")
    load = 0.5 + 1.3 * np.exp(-((np.arange(96)-40)**2)/40) + np.exp(-((np.arange(96)-72)**2)/40)
    dummy_load = pd.Series(load, index=idx)

    """
        Importing real data: one day with quarter-hourly prices

    """
    file_location = "20260112_Load_Germany_2026.xlsx"
    df_load = pd.read_excel(file_location,sheet_name='Load', header=0, index_col='DateTime', parse_dates=['DateTime'])
    load = pd.Series(df_load['Load (MW)'],index=df_load.index)
    load_scaled = load/df_load['Load (MW)'].max()

    # Optimization
    df, cap_mwh, power_mw = run_load_shifting_optimization(load_scaled)

    """
         Some Plots, directly with a subplot to use a combined x-axis (time)
     
    """
    fig, axes = plt.subplots(3, 1, figsize=(14, 12), sharex=True) # 3 Subplots, shared axis
    plt.rcParams.update({'font.size': 14}) # font size

    # Plot 1: Load Shifting (Original vs Net)
    axes[0].plot(df.index, df['Customer_Load_MW'], color='blue', linewidth=2, linestyle='--', label='Customer load (unoptimized)')
    axes[0].plot(df.index, df['Net_Grid_Load_MW'], color='darkblue', linewidth=2, label='Net grid load (shaved)')
    axes[0].set_title('Battery optimization results (load)')
    axes[0].set_ylabel('Power (MW)')
    axes[0].grid(True, linestyle=':', alpha=0.7)
    axes[0].legend()

    # Plot 2: Charge / Discharge Power (Symmetric around zero)
    axes[1].plot(df.index, df['Charge_MW'], color='green', label='Charge (MW)')
    axes[1].plot(df.index, -df['Discharge_MW'], color='red', label='Discharge (MW)') 
    axes[1].set_ylabel('Power (MW)')
    axes[1].axhline(0, color='grey', linestyle='--', linewidth=0.8)
    axes[1].fill_between(df.index, 0, df['Charge_MW'], color='green', alpha=0.2)
    axes[1].fill_between(df.index, 0, -df['Discharge_MW'], color='red', alpha=0.2)
    #axes[1].set_ylim(-power_mw *1.2, power_mw * 1.2)
    axes[1].grid(True, linestyle=':', alpha=0.7)
    axes[1].legend()

    # Plot 3: State of Charge (SoC)
    axes[2].plot(df.index, df['SoC_MWh'], color='purple', label='SoC (MWh)')
    axes[2].set_ylabel('SoC (MWh)')
    axes[2].set_ylim(0, cap_mwh * 1.1)
    axes[2].axhline(cap_mwh, color='grey', linestyle='--', label='Max. Capacity')
    axes[2].fill_between(df.index, 0, df['SoC_MWh'], color='purple', alpha=0.2)
    axes[2].grid(True, linestyle=':', alpha=0.7)
    axes[2].legend()

    # Format axis
    axes[2].xaxis.set_major_formatter(mdates.DateFormatter('%d/%m/%y'))
    plt.xlabel('Time')
    plt.tight_layout() 
    plt.show()
    
    
    fig, axes = plt.subplots(1, 1, figsize=(14, 6), sharex=True) # 3 Subplots, shared axis
    plt.rcParams.update({'font.size': 14}) # font size

    # Plot 1: Just the original load again
    axes.plot(df.index, df['Customer_Load_MW'], color='blue', linewidth=2, linestyle='--', label='Customer load (unoptimized)')
    #axes.plot(df.index, df['Net_Grid_Load_MW'], color='darkblue', linewidth=2, label='Net grid load (shaved)')
    axes.set_title('Load example')
    axes.set_ylabel('Power (MW)')
    axes.grid(True, linestyle=':', alpha=0.7)
    axes.legend()

  
    plt.xlabel('Time')
    plt.tight_layout() 
    plt.show()
    
