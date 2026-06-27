package com.calculocorridas.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.calculocorridas.presentation.screens.dashboard.DashboardScreen
import com.calculocorridas.presentation.screens.developer.DeveloperSettingsScreen
import com.calculocorridas.presentation.screens.developer.InspectorScreen
import com.calculocorridas.presentation.screens.history.HistoryScreen
import com.calculocorridas.presentation.screens.home.HomeScreen
import com.calculocorridas.presentation.screens.rules.RulesScreen
import com.calculocorridas.presentation.screens.settings.SettingsScreen
import com.calculocorridas.presentation.screens.subscription.SubscriptionScreen

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home         : Screen("home", "Home", Icons.Default.Home)
    object Dashboard    : Screen("dashboard", "Dashboard", Icons.Default.BarChart)
    object History      : Screen("history", "Histórico", Icons.Default.History)
    object Rules        : Screen("rules", "Regras", Icons.Default.Rule)
    object Settings     : Screen("settings", "Config", Icons.Default.Settings)
    object Subscription  : Screen("subscription",  "Pro",          Icons.Default.Settings)
    object Developer     : Screen("developer",     "Desenvolvedor", Icons.Default.Settings)
    object Inspector     : Screen("inspector",     "Inspector",     Icons.Default.Settings)
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Dashboard,
    Screen.History,
    Screen.Rules,
    Screen.Settings
)

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute != Screen.Subscription.route &&
            currentRoute != Screen.Developer.route &&
            currentRoute != Screen.Inspector.route) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(screen.icon, contentDescription = screen.label) },
                            label = { Text(screen.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(onNavigateHistory = { navController.navigate(Screen.History.route) })
            }
            composable(Screen.Dashboard.route) { DashboardScreen() }
            composable(Screen.History.route) { HistoryScreen() }
            composable(Screen.Rules.route) { RulesScreen() }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateSubscription = { navController.navigate(Screen.Subscription.route) },
                    onNavigateDeveloper    = { navController.navigate(Screen.Developer.route) }
                )
            }
            composable(Screen.Subscription.route) {
                SubscriptionScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.Developer.route) {
                DeveloperSettingsScreen(
                    onBack              = { navController.popBackStack() },
                    onNavigateInspector = { navController.navigate(Screen.Inspector.route) }
                )
            }
            composable(Screen.Inspector.route) {
                InspectorScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
