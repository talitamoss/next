package com.domain.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.domain.app.ui.contacts.ContactsScreen
import com.domain.app.ui.dashboard.DashboardScreen
import com.domain.app.ui.data.DataScreen
import com.domain.app.ui.settings.SettingsScreen
import com.domain.app.ui.social.SocialFeedScreen
import com.domain.app.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main Activity - Single Activity Architecture with Compose Navigation
 * 
 * File location: app/src/main/java/com/domain/app/MainActivity.kt
 */
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
    
    // Define bottom navigation items
    val bottomNavItems = listOf(
        BottomNavItem(
            route = Screen.Dashboard.route,
            icon = Icons.Default.Home,
            label = "Dashboard"
        ),
        BottomNavItem(
            route = Screen.Data.route,
            icon = Icons.Default.BarChart,
            label = "Data"
        ),
        BottomNavItem(
            route = Screen.Social.route,
            icon = Icons.Default.People,
            label = "Social"
        ),
        BottomNavItem(
            route = Screen.Settings.route,
            icon = Icons.Default.Settings,
            label = "Settings"
        )
    )
    
    // Determine if we should show bottom bar
    val showBottomBar = currentDestination?.hierarchy?.any { destination ->
        bottomNavItems.any { it.route == destination.route }
    } == true
    
    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { 
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label
                                )
                            },
                            label = { 
                                Text(
                                    text = item.label,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            selected = currentDestination?.hierarchy?.any { 
                                it.route == item.route 
                            } == true,
                            onClick = {
                                navController.navigate(item.route) {
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
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            // Main screens (with bottom nav)
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    navController = navController
                )
            }
            
            composable(Screen.Data.route) {
                DataScreen(
                    navController = navController
                )
            }
            
            composable(Screen.Social.route) {
                SocialFeedScreen(
                    navController = navController
                )
            }
            
            composable(Screen.Settings.route) {
                SettingsScreen(
                    navController = navController
                )
            }
        }
    }
}

/**
 * Bottom navigation item data
 */
data class BottomNavItem(
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String
)
