package com.samtothestreets.presentation.screens.privacy

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PrivacyScreen(onAccept: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Offline First.\nPrivacy Always.",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = "SAM to the Streets does NOT communicate with the internet. All case memory, scenario data, and AI interactions run entirely on this device.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        Button(
            onClick = onAccept,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("I Understand & Agree")
        }
    }
}
