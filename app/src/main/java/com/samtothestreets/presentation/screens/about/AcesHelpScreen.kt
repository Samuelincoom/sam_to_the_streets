package com.samtothestreets.presentation.screens.about

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcesHelpScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("How this helps in ACES") },
                navigationIcon = {
                    Button(onClick = onBack) { Text("Back") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "ACES 3b, Solar PV Systems and Microgrid",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Helps with load profiles, microgrid demand patterns, flexbility thinking, storage sizing questions, KPIs, and making sense of SAM style result tables. The microgrid course explicitly covers microgrid design, demand estimation, demand response, storage sizing, modelling workshops, KPIs, and an introduction to System Advisor Model (SAM).", // Typo: flexbility
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Text(
                text = "ACES 3c, Energy Markets, Systems and Python",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Helps with prices, volatility, correlations, flexibility, asset thinking, case studies, and turning data into graphs and questions before or alongside Python work. Our 3c material explicitly focuses on energy market dynamics, stochastic prices and weather, asset valuation under uncertainty, technical and economic power system modelling, flexibility, experimentation, and case studies.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Text(
                text = "ACES 3d, Wind Energy Systems",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Helps with imported wind related datasets, variability, time series patterns, comparisons, and explaining what stands out in graphs. The uploaded ACES materials clearly identify 3d as the Wind Energy Systems specialisation, so this is focused purely on making those files easier to read.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 32.dp)
            )
        }
    }
}
