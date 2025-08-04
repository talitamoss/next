// app/src/main/java/com/domain/app/MainActivity.kt
package com.domain.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import com.domain.app.ui.dashboard.DashboardScreen
import com.domain.app.ui.data.DataScreen
import com.domain.app.ui.settings.SettingsScreen
import com.domain.app.ui.security.PluginSecurityScreen
import com.domain.app.ui.security.SecurityAuditScreen
import com.domain.app.ui.theme.AppTheme
import com.domain.app.ui.theme.AppIcons
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                MainAppNavigation()
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainAppNavigation() {
    val navController = rememberNavController()
    val pagerState = rememberPagerState(pageCount = { 4 })
    val coroutineScope = rememberCoroutineScope()
    
    // Define the main tabs
    val mainTabs = listOf(
        TabItem("Dashboard", AppIcons.Navigation.dashboard),
        TabItem("Social", AppIcons.Navigation.social), // Using health icon as noted in Screen.kt
        TabItem("Data", AppIcons.Navigation.data),
        TabItem("Settings", AppIcons.Navigation.settings)
    )
    
    // Check if we're on a modal screen (plugin_security or security_audit)
    val isModalScreen = navController.currentBackStackEntry?.destination?.route?.let { route ->
        route.startsWith("plugin_security") || route == "security_audit"
    } ?: false
    
    if (isModalScreen) {
        // Show modal screens using standard navigation
        NavHost(
            navController = navController,
            startDestination = "main_tabs"
        ) {
            composable("main_tabs") {
                MainTabsWithPager(mainTabs, pagerState, navController, coroutineScope)
            }
            
            // Plugin security details screen
            composable(
                route = "plugin_security/{pluginId}",
                arguments = listOf(
                    navArgument("pluginId") { type = NavType.StringType }
                )
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
    } else {
        // Show main tabs with swipe functionality
        MainTabsWithPager(mainTabs, pagerState, navController, coroutineScope)
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainTabsWithPager(
    tabs: List<TabItem>,
    pagerState: androidx.compose.foundation.pager.PagerState,
    navController: androidx.navigation.NavController,
    coroutineScope: kotlinx.coroutines.CoroutineScope
) {
    Scaffold(
        bottomBar = {
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = { 
                            Text(
                                text = tab.title,
                                fontWeight = if (pagerState.currentPage == index) {
                                    FontWeight.Bold
                                } else {
                                    FontWeight.Normal
                                }
                            ) 
                        },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title
                            )
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) { page ->
            when (page) {
                0 -> DashboardScreen(navController)
                1 -> SocialPlaceholderScreen(navController)
                2 -> DataScreen(navController)
                3 -> SettingsScreen(navController)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialPlaceholderScreen(navController: androidx.navigation.NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Social Feed") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Column(
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Social Feed Coming Soon",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "This is a placeholder screen.\nThe real SocialFeedScreen will be built here.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Data class for tab configuration
data class TabItem(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
