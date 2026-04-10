package com.samtothestreets.presentation.screens.home

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.samtothestreets.data.entity.ProjectCase
import com.samtothestreets.data.importing.CsvParser
import com.samtothestreets.data.importing.SamParser
import com.samtothestreets.data.importing.XlsxParser
import com.samtothestreets.ai.LocalModelManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    cases: List<ProjectCase>,
    onCaseClick: (String) -> Unit,
    onCopilotClick: () -> Unit,
    onSetupClick: () -> Unit,
    onAboutClick: () -> Unit,
    onImportSuccess: (ProjectCase) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isImporting by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            isImporting = true
            coroutineScope.launch {
                try {
                    val mime = context.contentResolver.getType(uri) ?: ""
                    val isExcel = mime.contains("spreadsheet") || mime.contains("excel") || uri.path?.endsWith(".xlsx") == true
                    val isSam = uri.path?.endsWith(".sam") == true || uri.lastPathSegment?.endsWith(".sam") == true
                    val parsed = if (isSam) {
                        SamParser.parseSam(context, uri)
                    } else if (isExcel) {
                        XlsxParser.parseXlsx(context, uri)
                    } else {
                        CsvParser.parseCsv(context, uri)
                    }
                    onImportSuccess(parsed)
                    Toast.makeText(context, "Import successful!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    isImporting = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SAM for the streets") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = onSetupClick) {
                        Text("AI Setup")
                    }
                    IconButton(onClick = onAboutClick) {
                        Text("About")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { 
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse("https://github.com/Samuelincoom"))
                    context.startActivity(intent)
                },
                content = { Text("Fork this") }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Text(
                text = "Open a file. See the graph. Understand the story.",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )

            Row(modifier = Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { }, label = { Text("Offline Only", color = Color.Green) })
                val diag by LocalModelManager.diagnosticsFlow.collectAsState()
                val modeLabel = "Mode: ${diag.activeMode.displayName}"
                val aiColor = if (diag.activeMode != com.samtothestreets.ai.ModelMode.RULES_ONLY) Color.Green else Color.Gray
                AssistChip(onClick = { onSetupClick() }, label = { Text(modeLabel, color = aiColor) })
            }
            
            Row(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Button(onClick = onCopilotClick, modifier = Modifier.weight(1f)) {
                    Text("Python Helper")
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(onClick = { 
                    filePickerLauncher.launch(arrayOf("text/comma-separated-values", "text/csv", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "*/*"))
                }, modifier = Modifier.weight(1f), enabled = !isImporting) {
                    Text(if (isImporting) "Importing..." else "Import File (.csv/.xlsx/.sam)")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Local Case Memory", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(cases) { pCase ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCaseClick(pCase.id) },
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(pCase.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text(pCase.description, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                            Text("Tags: ${pCase.tags}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
                            if (pCase.id.contains("demo")) {
                                Text("* Demo Dataset", style = MaterialTheme.typography.labelSmall, color = Color.Red, modifier = Modifier.padding(top = 8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
