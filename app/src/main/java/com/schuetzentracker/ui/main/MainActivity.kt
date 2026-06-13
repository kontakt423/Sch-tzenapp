package com.schuetzentracker.ui.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.schuetzentracker.ui.analysis.TargetAnalysisScreen
import com.schuetzentracker.ui.camera.CameraScreen
import com.schuetzentracker.ui.diary.DiaryScreen
import com.schuetzentracker.ui.diary.SessionDetailScreen
import com.schuetzentracker.ui.home.HomeScreen
import com.schuetzentracker.ui.profile.ProfileScreen
import com.schuetzentracker.ui.settings.SettingsScreen
import com.schuetzentracker.ui.settings.DisciplineManagementScreen
import com.schuetzentracker.ui.statistics.StatisticsScreen
import com.schuetzentracker.ui.training.NewTrainingScreen
import com.schuetzentracker.ui.theme.SchutzenTrackerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SchutzenTrackerTheme {
                SchutzenTrackerApp()
            }
        }
    }
}

// ────────────────────────────────────────────────
// NAVIGATION ROUTES
// ────────────────────────────────────────────────

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Diary : Screen("diary")
    object NewTraining : Screen("new_training")
    object Statistics : Screen("statistics")
    object Profile : Screen("profile")
    object Camera : Screen("camera")
    object Settings : Screen("settings")
    object Disciplines : Screen("disciplines")
    object SessionDetail : Screen("session/{sessionId}") {
        fun createRoute(id: Long) = "session/$id"
    }
    object TargetAnalysis : Screen("analysis/{seriesId}") {
        fun createRoute(seriesId: Long) = "analysis/$seriesId"
    }
}

// ────────────────────────────────────────────────
// MAIN APP COMPOSABLE
// ────────────────────────────────────────────────

@Composable
fun SchutzenTrackerApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val topLevelRoutes = setOf(
        Screen.Home.route,
        Screen.Diary.route,
        Screen.Statistics.route,
        Screen.Profile.route
    )
    val showBottomBar = currentRoute in topLevelRoutes
    val showFab = currentRoute == Screen.Home.route || currentRoute == Screen.Diary.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    listOf(
                        Triple(Screen.Home, "Start", Icons.Default.Home),
                        Triple(Screen.Diary, "Tagebuch", Icons.Default.MenuBook),
                        Triple(Screen.Statistics, "Statistik", Icons.Default.BarChart),
                        Triple(Screen.Profile, "Profil", Icons.Default.Person)
                    ).forEach { (screen, label, icon) ->
                        NavigationBarItem(
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (showFab) {
                FloatingActionButton(
                    onClick = { navController.navigate(Screen.NewTraining.route) },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, "Neues Training")
                }
            }
        }
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

// ────────────────────────────────────────────────
// NAV HOST
// ────────────────────────────────────────────────

@Composable
fun AppNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onStartTraining = { navController.navigate(Screen.NewTraining.route) },
                onViewSession = { },
                onOpenSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.Diary.route) {
            DiaryScreen(
                onSessionClick = { id -> navController.navigate(Screen.SessionDetail.createRoute(id)) }
            )
        }

        composable(Screen.NewTraining.route) {
            NewTrainingScreen(
                onSaved = { navController.popBackStack() },
                onAnalyzeTarget = { seriesId ->
                    navController.navigate(Screen.TargetAnalysis.createRoute(seriesId))
                },
                onOpenCamera = { navController.navigate(Screen.Camera.route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Statistics.route) {
            StatisticsScreen()
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateToDisciplines = { navController.navigate(Screen.Disciplines.route) }
            )
        }

        composable(Screen.Disciplines.route) {
            DisciplineManagementScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable("session/{sessionId}") { backStack ->
            val sessionId = backStack.arguments?.getString("sessionId")?.toLongOrNull() ?: 0L
            SessionDetailScreen(
                sessionId = sessionId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Camera.route) {
            CameraScreen(
                onPhotoTaken = { uri ->
                    // URI zurück an NewTraining übergeben (via BackStack)
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("camera_uri", uri.toString())
                    navController.popBackStack()
                },
                onClose = { navController.popBackStack() }
            )
        }

        composable("analysis/{seriesId}") { backStack ->
            val seriesId = backStack.arguments?.getString("seriesId")?.toLongOrNull() ?: 0L
            TargetAnalysisScreen(
                seriesId = seriesId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
