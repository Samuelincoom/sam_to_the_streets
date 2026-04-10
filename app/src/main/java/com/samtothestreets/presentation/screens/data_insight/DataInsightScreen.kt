package com.samtothestreets.presentation.screens.data_insight

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.samtothestreets.data.AppDatabase
import com.samtothestreets.data.entity.GraphQA
import com.samtothestreets.data.entity.ProjectCase
import com.samtothestreets.domain.PythonTemplateEngine
import com.samtothestreets.domain.parsing.GraphSuggestionEngine
import com.samtothestreets.domain.parsing.GridStressEngine
import com.samtothestreets.domain.schema.DatasetSchema
import com.samtothestreets.presentation.components.HeinenConceptualChart
import com.samtothestreets.ai.LocalModelManager
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataInsightScreen(
    projectCase: ProjectCase?,
    onBack: () -> Unit
) {
    if (projectCase == null) return
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val dao = remember { AppDatabase.getDatabase(context).graphQADao() }
    
    val schema = remember(projectCase) { DatasetSchema.fromJson(projectCase.serializedDataset) }
    val defaultGraph = remember(schema) { GraphSuggestionEngine.suggestDefaultGraph(schema) }

    var selectedYAxis by remember { mutableStateOf(defaultGraph.yAxisName ?: "") }
    
    val numericColumns = schema.columns.filter { it.type == com.samtothestreets.domain.schema.ColumnType.NUMERIC }
    if (selectedYAxis.isEmpty() && numericColumns.isNotEmpty()) {
        selectedYAxis = numericColumns.first().name
    }

    var questionInput by remember { mutableStateOf("") }
    var currentAnswer by remember { mutableStateOf<String?>(null) }
    var currentAnswerId by remember { mutableStateOf<String?>(null) }
    var savedHistory by remember { mutableStateOf<List<GraphQA>>(emptyList()) }
    var showPython by remember { mutableStateOf(false) }
    var pythonSnippet by remember { mutableStateOf<Pair<String,String>?>(null) }
    var showGridStress by remember { mutableStateOf(false) }

    LaunchedEffect(projectCase.id) {
        LocalModelManager.resetSession()
        dao.getQAForCase(projectCase.id).collect {
            savedHistory = it
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            LocalModelManager.resetSession()
        }
    }

    fun submitQuestion(q: String) {
        val yCol = numericColumns.find { it.name == selectedYAxis }
        val deterministicText = if (yCol != null) {
            when {
                q.contains("up or down", ignoreCase = true) -> {
                    val first = yCol.numericValues.firstOrNull() ?: 0f
                    val last = yCol.numericValues.lastOrNull() ?: 0f
                    if (last > first) "The overall trend is going UP (started at $first, ended at $last)." else "The overall trend is going DOWN (started at $first, ended at $last)."
                }
                q.contains("peak", ignoreCase = true) -> "The peak value is ${yCol.max()}."
                q.contains("volatile", ignoreCase = true) -> "Spread range is ${yCol.max() - yCol.min()}."
                else -> "${yCol.name} has an average of ${"%.2f".format(yCol.avg())} ranging from ${yCol.min()} to ${yCol.max()}"
            }
        } else "Data metric is not fully numerical."
        
        val llmActive = LocalModelManager.diagnostics.activeMode != com.samtothestreets.ai.ModelMode.RULES_ONLY    
        currentAnswer = "Generating..."
        
        coroutineScope.launch {
            val finalAnswer = if (llmActive) {
                LocalModelManager.generateResponse(
                    prompt = "User asks: '$q' about the graph. The exact mathematical truth is: '$deterministicText'. Reply simply and conversationally to explain this.",
                    contextInfo = "Screen: DataInsight, Dataset: ${projectCase.id}"
                )
            } else {
                "[Rules Mode] $deterministicText"
            }
            currentAnswer = finalAnswer
            currentAnswerId = UUID.randomUUID().toString()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(projectCase.title) },
                navigationIcon = {
                    Button(onClick = onBack) { Text("Back") }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            item {
                Text("This graph is the starting point. Ask what it shows, what stands out, and what to compare next.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                if (numericColumns.isEmpty()) {
                    Text("No numeric data available to graph in this dataset.")
                } else {
                    val yCol = numericColumns.find { it.name == selectedYAxis }
                    if (yCol != null) {
                        HeinenConceptualChart(
                            title = "$selectedYAxis",
                            explanation = "Auto-plot for column: $selectedYAxis",
                            yValues = yCol.numericValues,
                            yLabel = selectedYAxis
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    if (yCol != null) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Min: ${yCol.min()}", style = MaterialTheme.typography.labelMedium)
                            Text("Avg: ${"%.2f".format(yCol.avg())}", style = MaterialTheme.typography.labelMedium)
                            Text("Max: ${yCol.max()}", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        numericColumns.take(5).forEach { col ->
                            FilterChip(
                                selected = selectedYAxis == col.name,
                                onClick = { selectedYAxis = col.name },
                                label = { Text(col.name) }
                            )
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            }

            item {
                Text("Ask anything about this graph", fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = questionInput,
                    onValueChange = { questionInput = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    placeholder = { Text("E.g. Why is this rising?") },
                    trailingIcon = {
                        Button(onClick = { submitQuestion(questionInput) }, enabled = questionInput.isNotBlank()) {
                            Text("Ask")
                        }
                    }
                )
                
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val starters = listOf("Why is this rising?", "Why is there a peak here?", "What changed after this point?", "Which part is most volatile?", "What does this mean for battery sizing?", "What does this suggest for a microgrid?", "What should I compare next?")
                    starters.forEach { suggestion ->
                        AssistChip(onClick = { questionInput = suggestion; submitQuestion(suggestion) }, label = { Text(suggestion) })
                    }
                }
                
                if (currentAnswer != null) {
                    Card(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(currentAnswer ?: "")
                            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = {
                                    coroutineScope.launch {
                                        dao.insertGraphQA(GraphQA(id = currentAnswerId!!, caseId = projectCase.id, question = questionInput, answer = currentAnswer!!, wasUseful = true))
                                    }
                                }) {
                                    Icon(Icons.Default.ThumbUp, contentDescription = "Useful", modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Save this explanation")
                                }
                            }
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            }

            item {
                Text("Flexibility & Grid Stress Lens", fontWeight = FontWeight.Bold)
                Text("This graph/data-driven insight helps identify peak demand windows and steep ramp periods to highlight where local flexibility matters. This is not a full network simulation.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(top = 4.dp))
                
                Button(onClick = { showGridStress = !showGridStress }, modifier = Modifier.padding(top = 8.dp)) {
                    Text(if (showGridStress) "Hide Grid Stress Lens" else "Run Flexibility Analysis")
                }

                if (showGridStress) {
                    val stressInsight = GridStressEngine.analyzeFlexibility(schema, selectedYAxis)
                    if (stressInsight != null) {
                        Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(stressInsight.title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text(stressInsight.description, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    } else {
                        Text("Numeric column required for flexibility analysis.", modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.error)
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            }

            item {
                Text("Python is here to help me continue the same analysis on laptop later, not to replace the graph.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                Button(onClick = { 
                    showPython = !showPython 
                    if (showPython) {
                        pythonSnippet = PythonTemplateEngine().generateSnippet("trend $selectedYAxis", projectCase, false)
                    }
                }, modifier = Modifier.padding(top = 8.dp)) {
                    Text(if (showPython) "Hide Python" else "Show related Python")
                }
                
                if (showPython && pythonSnippet != null) {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Text(pythonSnippet!!.first, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(8.dp))
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            }
            
            if (savedHistory.isNotEmpty()) {
                item {
                    Text("Saved Explanations", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                }
                items(savedHistory) { qa ->
                    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Q: ${qa.question}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                            Text(qa.answer, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        }
    }
}
