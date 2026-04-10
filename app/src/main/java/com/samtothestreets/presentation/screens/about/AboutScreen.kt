package com.samtothestreets.presentation.screens.about

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onNavigateToAcesHelp: () -> Unit,
    onNavigateToCredits: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About") },
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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SAM for the streets",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "Built by Samuel",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            Text(
                text = "This app is a live experiment in using local Gemma language models for ACES analysis, fully offline on a phone.\n\nThe idea is simple: open energy data, see the graph, ask questions, and understand the result without sending your files to onliine AI services.", // typo: onliine
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Start,
                modifier = Modifier.padding(bottom = 16.dp).fillMaxWidth()
            )

            Text(
                text = "This app does not use internet permission for its core work, does not upload your datasets to a server, and does not give me, Google, or any company access to your class data or your phone files through the app.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Start,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(bottom = 16.dp).fillMaxWidth()
            )

            Text(
                text = "If enough of us keep using it with local datasets, saved explanations, and corrections, we could later combine approved local knowledge into a much bigger Flensburg and Germany focused ACES analysis system, still without giving our data away.\n\nSo yes, the long term dream is a kind of local ACES AI monster. The polite version is \"shared research assistant.\"",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Start,
                modifier = Modifier.padding(bottom = 16.dp).fillMaxWidth()
            )

            Text(
                text = "This app is built mainly with Kotlin and some Java. I am much stronger in Kotlin and Java than in Python, so the Python side here is meant as a practical helper layer, not me pretending to be a Python wizard.\n\nIf it helps you suffer less while reading weird energy data, then it is doing its job.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Start,
                modifier = Modifier.padding(bottom = 32.dp).fillMaxWidth()
            )

            Divider(modifier = Modifier.padding(bottom = 16.dp))

            Button(
                onClick = onNavigateToAcesHelp,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Text("How this helps in ACES")
            }

            Button(
                onClick = onNavigateToCredits,
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
            ) {
                Text("Libraries & Credits")
            }
        }
    }
}
