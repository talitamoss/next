// app/src/main/java/com/domain/app/MainActivity.kt
package com.domain.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.domain.app.ui.dashboard.DashboardScreen
import com.domain.app.ui.reflect.ReflectCalendarScreen
import com.domain.app.ui.reflect.ReflectDayDetailScreen
import com.domain.app.ui.settings.SettingsScreen
import com.domain.app.ui.theme.AppTheme
import com.domain.app.ui.theme.AppIcons
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.LocalDate

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                MainNavigation()
            }
        }
    }
}

@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        composable("main") {
            MainScreen(
                onNavigateToSettings = {
                    navController.navigate("settings")
                },
                onNavigateToDayDetail = { date ->
                    navController.navigate("day_detail/${date.toString()}")
                }
            )
        }
        
        composable("settings") {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToPlugins = {
                    // TODO: Navigate to plugins management
                },
                onNavigateToSecurity = {
                    // TODO: Navigate to security settings
                },
                onNavigateToAbout = {
                    // TODO: Navigate to about
                }
            )
        }
        
        composable(
            route = "day_detail/{date}",
            arguments = listOf(navArgument("date") { type = NavType.StringType })
        ) { backStackEntry ->
            val dateStr = backStackEntry.arguments?.getString("date") ?: ""
            val date = try {
                LocalDate.parse(dateStr)
            } catch (e: Exception) {
                LocalDate.now()
            }
            
            ReflectDayDetailScreen(
                date = date,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToDayDetail: (LocalDate) -> Unit
) {
    val pagerState = rememberPagerState(initialPage = 0) { 2 }
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) }
    
    // Sync pager with bottom navigation
    LaunchedEffect(pagerState.currentPage) {
        selectedTab = pagerState.currentPage
    }
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(0)
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = AppIcons.Navigation.home,
                            contentDescription = "Collect"
                        )
                    },
                    label = { Text("Collect") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(1)
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = AppIcons.Data.chart,
                            contentDescription = "Reflect"
                        )
                    },
                    label = { Text("Reflect") }
                )
            }
        }
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            beyondBoundsPageCount = 1
        ) { page ->
            when (page) {
                0 -> DashboardScreen(
                    onNavigateToPlugin = { plugin ->
                        // TODO: Navigate to plugin detail screen
                    },
                    onNavigateToSettings = onNavigateToSettings
                )
                1 -> ReflectCalendarScreen(
                    onNavigateToSettings = onNavigateToSettings,
                    onNavigateToDayDetail = onNavigateToDayDetail
                )
            }
        }
    }
}
