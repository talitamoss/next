package com.domain.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.domain.app.ui.dashboard.DashboardScreen
import com.domain.app.ui.data.DataScreen
import com.domain.app.ui.settings.SettingsScreen
import com.domain.app.social.ui.feed.SocialFeedScreen
import com.domain.app.ui.theme.AppTheme

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

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val currentRoute = currentRoute(navController)

    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController = navController, currentRoute = currentRoute)
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("dashboard") { DashboardScreen(navController) }
            composable("data") { DataScreen(navController) }
            composable("social") { SocialFeedScreen() } // Social feed doesn't need navController yet
            composable("settings") { SettingsScreen(navController) }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController, currentRoute: String?) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
            label = { Text("Dashboard") },
            selected = currentRoute == "dashboard",
            onClick = { navController.navigate("dashboard") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Analytics, contentDescription = "Data") },
            label = { Text("Data") },
            selected = currentRoute == "data",
            onClick = { navController.navigate("data") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.People, contentDescription = "Social") },
            label = { Text("Social") },
            selected = currentRoute == "social",
            onClick = { navController.navigate("social") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings") },
            selected = currentRoute == "settings",
            onClick = { navController.navigate("settings") }
        )
    }
}

@Composable
fun currentRoute(navController: NavHostController): String? {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    return navBackStackEntry?.destination?.route
}
