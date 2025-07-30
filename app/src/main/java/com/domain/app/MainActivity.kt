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
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.domain.app.ui.contacts.AddContactScreen
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
                                    // Pop up to the start destination of the graph to
                                    // avoid building up a large stack of destinations
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    // Avoid multiple copies of the same destination
                                    launchSingleTop = true
                                    // Restore state when reselecting a previously selected item
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
                    onNavigateToPlugin = { pluginId ->
                        // Navigate to plugin detail if needed
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    }
                )
            }
            
            composable(Screen.Data.route) {
                DataScreen(
                    onNavigateToDetail = { dataPointId ->
                        // Navigate to data detail if needed
                    }
                )
            }
            
            composable(Screen.Social.route) {
                SocialFeedScreen(
                    onNavigateToContacts = {
                        navController.navigate(Screen.Contacts.route)
                    },
                    onNavigateToAddContact = {
                        navController.navigate(Screen.AddContact.route)
                    },
                    onNavigateToContactDetail = { contactId ->
                        navController.navigate(Screen.ContactDetail.createRoute(contactId))
                    }
                )
            }
            
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            
            // Sub-screens (without bottom nav)
            composable(Screen.Contacts.route) {
                ContactsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToAddContact = {
                        navController.navigate(Screen.AddContact.route)
                    },
                    onNavigateToContactDetail = { contactId ->
                        navController.navigate(Screen.ContactDetail.createRoute(contactId))
                    }
                )
            }
            
            composable(Screen.AddContact.route) {
                AddContactScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onContactAdded = { contactId ->
                        // Navigate back to contacts or to the new contact detail
                        navController.popBackStack()
                    }
                )
            }
            
            composable(
                route = Screen.ContactDetail.route,
                arguments = Screen.ContactDetail.arguments
            ) { backStackEntry ->
                val contactId = backStackEntry.arguments?.getString("contactId") ?: ""
                
                // TODO: Create ContactDetailScreen
                PlaceholderScreen(
                    title = "Contact Detail",
                    subtitle = "Contact ID: $contactId",
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

/**
 * Placeholder screen for unimplemented screens
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceholderScreen(
    title: String,
    subtitle: String = "",
    onNavigateBack: (() -> Unit)? = null
) {
    Scaffold(
        topBar = {
            if (onNavigateBack != null) {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, "Back")
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text(title) }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Construction,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium
                )
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "Coming Soon",
                    style = MaterialTheme.typography.bodyLarge
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

/**
 * Screen routes for navigation
 */
sealed class Screen(val route: String) {
    // Main screens (shown in bottom nav)
    object Dashboard : Screen("dashboard")
    object Data : Screen("data")
    object Social : Screen("social")
    object Settings : Screen("settings")
    
    // Sub screens
    object Contacts : Screen("contacts")
    object AddContact : Screen("add_contact")
    object ContactDetail : Screen("contact/{contactId}") {
        fun createRoute(contactId: String) = "contact/$contactId"
        val arguments = listOf(
            androidx.navigation.navArgument("contactId") {
                type = androidx.navigation.NavType.StringType
            }
        )
    }
}
