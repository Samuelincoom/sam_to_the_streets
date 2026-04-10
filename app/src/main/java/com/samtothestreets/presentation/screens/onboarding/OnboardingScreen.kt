package com.samtothestreets.presentation.screens.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    var step by remember { mutableStateOf(0) }

    val titles = listOf(
        "See your data instantly",
        "Private by default",
        "Local AI, not online AI",
        "The more local data, the more useful it gets",
        "Where this helps in ACES",
        "Python is the helper, not the hero",
        "The bigger idea"
    )

    val descriptions = listOf(
        "This app turns raw energy files into graphs, quick explanations, and useful follow-up quesitons in seconds.\n\nLess spreadsheet pain. More \"okay, now I actually get it.\"", // typo: quesitons
        "This app works locally on the phone.\n\nIt does not need cloud AI for the core experience, and it is designed so your class data does not have to leave your device.\n\nNo server drama. No mystery upload vibes.",
        "This app uses local Gemma models running on device to help explain graphs and datasets in simpler language.\n\nIf the local AI is unavailable, the app still works with deterministic analysis and rules mode.",
        "Every new dataset gives this app more local cases to compare, more patterns to recognize, and more useful saved explanations.\n\nThis means richer local case memory, better comparisons, and better future shared knowledge.",
        "This app is useful across ACES because the program is built around modelling, data handling, and analyzing results.\n\n• ACES 3b (Microgrid): Helps with load profiles, storage sizing, and exploring SAM style results.\n• ACES 3c (Energy Markets): Helps with pricing, volatility, correlations, and explaining case studies.\n• ACES 3d (Wind Systems): Helps with time series patterns, comparisons, and explaining imported wind datasets.",
        "Python is here to help continue the same analysis on a laptop later, not to replace the graph.\n\nThis app can generate practical Python starting points based on the exact dataset you are looking at right now. That fits ACES perfectly since the module emphasizes quantitative modelling in programming environments.",
        "If enough classmates use this app with local data and saved corrections, the long term goal is to combine approved local knowledge into a stronger Flensburg and Germany focused ACES analysis system.\n\nIn less dramatic words: a shared local research brain. Still local. Still privacy safe. Still not donated to random online AI companies."
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = titles[step],
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = descriptions[step],
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Start
        )
        Spacer(modifier = Modifier.height(48.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(
                onClick = { if (step > 0) step-- },
                enabled = step > 0
            ) {
                Text("Back")
            }
            
            Button(
                onClick = {
                    if (step < titles.size - 1) step++ else onComplete()
                }
            ) {
                Text(if (step == titles.size - 1) "Let's Go" else "Next")
            }
        }
    }
}
