package com.privacyguardian.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.privacyguardian.PrivacyGuardianApp
import com.privacyguardian.ui.guardian.GuardianScreen
import com.privacyguardian.ui.history.HistoryScreen
import com.privacyguardian.ui.home.HomeScreen
import com.privacyguardian.ui.result.ResultScreen
import com.privacyguardian.ui.scanner.ScanScreen
import com.privacyguardian.ui.scanner.ScanViewModel
import com.privacyguardian.ui.settings.SettingsScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    app: PrivacyGuardianApp,
    scanViewModel: ScanViewModel,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomRoutes = setOf(Screen.Home.route, Screen.Scan.route, Screen.Guardian.route, Screen.History.route)
    val showBottomBar = currentRoute in bottomRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                com.privacyguardian.ui.components.BottomNavBar(
                    currentRoute = currentRoute ?: Screen.Home.route,
                    onNavigate = { route ->
                        if (currentRoute != route) {
                            navController.navigate(route) {
                                popUpTo(Screen.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        app = app,
                        onNavigate = { route -> navController.navigate(route) },
                        scanViewModel = scanViewModel
                    )
                }
                composable(Screen.Scan.route) {
                    ScanScreen(
                        app = app,
                        scanViewModel = scanViewModel,
                        onResult = { navController.navigate(Screen.Result.route) },
                        onNavigate = { route -> navController.navigate(route) }
                    )
                }
                composable(Screen.Guardian.route) {
                    GuardianScreen(
                        app = app,
                        scanViewModel = scanViewModel,
                        onNavigate = { route -> navController.navigate(route) }
                    )
                }
                composable(Screen.History.route) {
                    HistoryScreen(app = app)
                }
                composable(Screen.Result.route) {
                    ResultScreen(
                        scanViewModel = scanViewModel,
                        onBackToHome = { navController.navigate(Screen.Home.route) { popUpTo(0) } },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(
                        app = app,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
