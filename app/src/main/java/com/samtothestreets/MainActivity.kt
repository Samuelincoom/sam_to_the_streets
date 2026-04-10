package com.samtothestreets

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.samtothestreets.data.AppDatabase
import com.samtothestreets.data.entity.ProjectCase
import com.samtothestreets.data.repository.CaseRepository
import com.samtothestreets.presentation.screens.data_insight.DataInsightScreen
import com.samtothestreets.presentation.screens.copilot.PythonCopilotScreen
import com.samtothestreets.presentation.screens.home.HomeScreen
import com.samtothestreets.presentation.screens.privacy.PrivacyScreen
import com.samtothestreets.presentation.theme.SAMToTheStreetsTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var repository: CaseRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val db = AppDatabase.getDatabase(this)
        repository = CaseRepository(db.caseDao(), db.graphQADao())

        // Inject generic demo data on first launch
        lifecycleScope.launch {
            com.samtothestreets.ai.LocalModelManager.tryAutoLoadBundledModel(this@MainActivity)
            if (repository.allCases.first().isEmpty()) {
                val demos = com.samtothestreets.data.assets.BuiltInDemosInjector.parseAndInjectDemos(this@MainActivity)
                for (demo in demos) {
                    repository.insertCaseConfig(demo)
                }
            }
        }

        setContent {
            SAMToTheStreetsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    var cases by remember { mutableStateOf<List<ProjectCase>>(emptyList()) }
                    var hasAcceptedPrivacy by remember { mutableStateOf(false) }

                    LaunchedEffect(Unit) {
                        repository.allCases.collect { cases = it }
                    }

                    NavHost(navController = navController, startDestination = "home") {
                        composable("about") {
                            com.samtothestreets.presentation.screens.about.AboutScreen(
                                onBack = { navController.popBackStack() },
                                onNavigateToAcesHelp = { navController.navigate("aces_help") },
                                onNavigateToCredits = { navController.navigate("libraries") }
                            )
                        }
                        composable("aces_help") {
                            com.samtothestreets.presentation.screens.about.AcesHelpScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("libraries") {
                            com.samtothestreets.presentation.screens.about.LibrariesCreditsScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("home") {
                            HomeScreen(
                                cases = cases,
                                onCaseClick = { caseId ->
                                    navController.navigate("case_detail/$caseId")
                                },
                                onCopilotClick = {
                                    navController.navigate("copilot")
                                },
                                onSetupClick = {
                                    navController.navigate("setup")
                                },
                                onAboutClick = {
                                    navController.navigate("about")
                                },
                                onImportSuccess = { newCase ->
                                    lifecycleScope.launch {
                                        val cDao = AppDatabase.getDatabase(this@MainActivity).caseDao()
                                        cDao.insertCase(newCase)
                                    }
                                }
                            )
                        }
                        composable("case_detail/{caseId}") { backStackEntry ->
                            val caseId = backStackEntry.arguments?.getString("caseId")
                            var projectCase by remember { mutableStateOf<ProjectCase?>(null) }

                            LaunchedEffect(caseId) {
                                if (caseId != null) {
                                    projectCase = repository.getCaseById(caseId)
                                }
                            }

                            DataInsightScreen(
                                projectCase = projectCase,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("copilot") {
                            val dbCase = cases.firstOrNull() 
                            PythonCopilotScreen(
                                currentCase = dbCase,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("setup") {
                            com.samtothestreets.presentation.screens.setup.SetupScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}

