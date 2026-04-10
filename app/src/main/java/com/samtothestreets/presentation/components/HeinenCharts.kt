package com.samtothestreets.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.FloatEntry

@Composable
fun HeinenConceptualChart(
    title: String,
    explanation: String,
    yValues: List<Float>,
    yLabel: String
) {
    if (yValues.isEmpty()) return

    val chartEntryModelProducer = remember {
        ChartEntryModelProducer(
            yValues.mapIndexed { index, value -> FloatEntry(x = index.toFloat(), y = value) }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            
            // Labeling appropriately based on user prompt criteria
            Text(
                text = "Disclaimer: Conceptual approximation. Not a full Pyomo/HiGHS solve.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Chart(
                chart = lineChart(),
                chartModelProducer = chartEntryModelProducer,
                startAxis = rememberStartAxis(title = yLabel),
                bottomAxis = rememberBottomAxis(title = "Time Index"),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("What am I looking at?", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
            Text(explanation, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Why this matters: Shows general profile alignment for classroom concepts without needing raw NDK optimization.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        }
    }
}
