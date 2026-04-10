package com.samtothestreets.presentation.screens.setup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.samtothestreets.ai.LocalModelManager
import com.samtothestreets.ai.ModelMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val diagnostics by LocalModelManager.diagnosticsFlow.collectAsState()
    var isLoading by remember { mutableStateOf(false) }
    var showAdvanced by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            isLoading = true
            coroutineScope.launch {
                LocalModelManager.loadModelFromUri(context, uri)
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LiteRT-LM Engine Diagnostics") },
                navigationIcon = {
                    Button(onClick = onBack) { Text("Back") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxWidth()) {
            Text("Active Mode", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            
            val statusColor = when (diagnostics.activeMode) {
                ModelMode.BUILT_IN -> MaterialTheme.colorScheme.primary
                ModelMode.USER_MODEL -> MaterialTheme.colorScheme.secondary
                ModelMode.RULES_ONLY -> MaterialTheme.colorScheme.error
            }
            Text(diagnostics.activeMode.displayName, color = statusColor, style = MaterialTheme.typography.headlineMedium)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("Diagnostic Readout", fontWeight = FontWeight.Bold)
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("LiteRT-LM runtime active: ${if (diagnostics.liteRtLmActive) "Yes" else "No"}")
                    Text("Current Backend: ${diagnostics.backend}")
                    Text("Built-in model found: ${if (diagnostics.builtInFound) "Yes" else "No"}")
                    Text("Copied from assets: ${if (diagnostics.copiedFromAssets) "Yes" else "No"}")
                    Text("Load success: ${if (diagnostics.loadSuccess) "Yes" else "No"}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Device Status: ${diagnostics.deviceStatusExplanation}", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.tertiary)
                    if (diagnostics.exactRuntimeModelPath != null) {
                        Text("Exact path: ${diagnostics.exactRuntimeModelPath}")
                    }
                    if (diagnostics.activeFilename != null) {
                        Text("Active filename: ${diagnostics.activeFilename}")
                    }
                    if (diagnostics.lastError != null) {
                        Text("Last Error: ${diagnostics.lastError}", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                CircularProgressIndicator()
                Text("Loading local model instance...")
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (diagnostics.activeMode != ModelMode.BUILT_IN && diagnostics.builtInFound) {
                        Button(onClick = {
                            isLoading = true
                            coroutineScope.launch {
                                LocalModelManager.forceRulesOnly() // Reset first
                                LocalModelManager.tryAutoLoadBundledModel(context, isManualRetry = true)
                                isLoading = false
                            }
                        }) {
                            Text("Use Built-in AI")
                        }
                    }

                    if (diagnostics.activeMode != ModelMode.RULES_ONLY) {
                        Button(onClick = { 
                            LocalModelManager.forceRulesOnly()
                        }) {
                            Text("Rules Only")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("Advanced Model Setup", fontWeight = FontWeight.Bold, modifier=Modifier.weight(1f))
                    Switch(checked = showAdvanced, onCheckedChange = { showAdvanced = it })
                }
                
                if (showAdvanced) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("You can replace the built-in model with a larger compatible '.litertlm' model from your device storage.", style = MaterialTheme.typography.bodySmall)
                    Button(onClick = { 
                        filePickerLauncher.launch(arrayOf("*/*"))
                    }, modifier = Modifier.padding(top = 8.dp)) {
                        Text("Use my own larger local model")
                    }
                }
            }
        }
    }
}
