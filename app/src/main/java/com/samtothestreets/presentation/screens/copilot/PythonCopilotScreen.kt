package com.samtothestreets.presentation.screens.copilot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.samtothestreets.data.entity.ProjectCase
import com.samtothestreets.domain.PythonTemplateEngine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PythonCopilotScreen(
    currentCase: ProjectCase?,
    onBack: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var snippet by remember { mutableStateOf("# Real snippets will appear here when generated") }
    var explanation by remember { mutableStateOf("Ready for generation.") }
    var isAdvanced by remember { mutableStateOf(false) }

    val templateEngine = remember { PythonTemplateEngine() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Python Co-Pilot") },
                navigationIcon = {
                    Button(onClick = onBack) { Text("Back") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Text("Python execution unavailable on this device. Code generation and explanation still work.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
            Spacer(modifier = Modifier.height(16.dp))

            Text("Selected Context: ${currentCase?.title ?: "None"}", fontWeight=FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("Advanced Mode:")
                Switch(checked = isAdvanced, onCheckedChange = { isAdvanced = it }, modifier = Modifier.padding(start=8.dp))
            }
            
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("E.g. cheapest scenario, plot time series, calculate volatility") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = { 
                    val res = templateEngine.generateSnippet(query, currentCase, isAdvanced)
                    snippet = res.first
                    explanation = "Why this code was generated:\n${res.second}"
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Generate Code Template")
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Generated Recipe:", fontWeight = FontWeight.Bold)
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF2B2B2B))
                    .padding(16.dp)
            ) {
                Text(
                    text = snippet,
                    color = Color(0xFFA9B7C6),
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(explanation, style = MaterialTheme.typography.bodySmall)
        }
    }
}
