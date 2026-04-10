package com.samtothestreets.presentation.screens.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibrariesCreditsScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Libraries & Credits") },
                navigationIcon = {
                    Button(onClick = onBack) { Text("Back") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
        ) {
            item { Spacer(modifier = Modifier.height(24.dp)) }

            item {
                Text(
                    text = "Core local AI",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                CreditLink(title = "LiteRT-LM", url = "https://github.com/google-ai-edge/LiteRT-LM")
                CreditLink(title = "LiteRT", url = "https://github.com/google-ai-edge/litert")
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                Text(
                    text = "Graphing",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                CreditLink(title = "Vico Charts", url = "https://github.com/patrykandpatrick/vico")
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                Text(
                    text = "File parsing",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                CreditLink(title = "Apache POI", url = "https://github.com/apache/poi")
                CreditLink(title = "OpenCSV", url = "https://github.com/gmarziou/open-csv")
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                Text(
                    text = "Developer",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                CreditLink(title = "My GitHub", url = "https://github.com/Samuelincoom")
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            item {
                Divider(modifier = Modifier.padding(vertical = 16.dp))
                Text(
                    text = "Built mainly with Kotlin and some Java. The Python support here is pactical and generated around real data workflows. I am stronger in Kotlin/Java than in Python, and that is okay.", // Typo: pactical
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
            }
        }
    }
}

@Composable
fun CreditLink(title: String, url: String) {
    val context = LocalContext.current
    Text(
        text = title,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.tertiary,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            }
            .padding(vertical = 8.dp)
    )
}
