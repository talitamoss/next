package com.domain.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.domain.app.ui.dashboard.DashboardScreen
import com.domain.app.ui.data.DataScreen
import com.domain.app.ui.settings.SettingsScreen
import com.domain.app.ui.security.PluginSecurityScreen
import com.domain.app.ui.security.SecurityAuditScreen
import com.domain.app.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    // Hide bottom bar on detail screens
    val showBottomBar = when (currentDestination?.route) {
        "plugin_security/{pluginId}" -> false
        "security_audit" -> false
        else -> true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    val items = listOf(
                        Screen.Dashboard,
                        Screen.Data,
                        Screen.Settings
                    )
                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = null) },
                            label = { Text(screen.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) { DashboardScreen(navController) }
            composable(Screen.Data.route) { DataScreen(navController) }
            composable(Screen.Settings.route) { SettingsScreen(navController) }
            
            // Plugin security detail screen
            composable(
                route = "plugin_security/{pluginId}",
                arguments = listOf(navArgument("pluginId") { type = NavType.StringType })
            ) { backStackEntry ->
                val pluginId = backStackEntry.arguments?.getString("pluginId") ?: ""
                PluginSecurityScreen(
                    pluginId = pluginId,
                    navController = navController
                )
            }
            
            // Security audit screen
            composable("security_audit") {
                SecurityAuditScreen(navController)
            }
        }
    }
}
