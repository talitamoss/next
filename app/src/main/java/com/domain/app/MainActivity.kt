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
import com.domain.app.ui.dashboard.DashboardScreen
import com.domain.app.ui.reflect.ReflectScreen  // Using original ReflectScreen for now
import com.domain.app.ui.settings.SettingsScreen
import com.domain.app.ui.theme.AppTheme
import com.domain.app.ui.theme.AppIcons
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen() {
    val pagerState = rememberPagerState(initialPage = 0) { 2 }
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }
    
    // Sync pager with bottom navigation
    LaunchedEffect(pagerState.currentPage) {
        selectedTab = pagerState.currentPage
    }
    
    if (showSettings) {
        SettingsScreen(
            onNavigateBack = {
                showSettings = false
            },
            onNavigateToPlugins = {
                // TODO: Navigate to plugins management screen
            },
            onNavigateToSecurity = {
                // TODO: Navigate to security settings screen
            },
            onNavigateToAbout = {
                // TODO: Navigate to about screen
            }
        )
    } else {
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
                    .padding(paddingValues)
            ) { page ->
                when (page) {
                    0 -> {
                        DashboardScreen(
                            onNavigateToPlugin = { plugin ->
                                // TODO: Navigate to plugin detail screen
                            },
                            onNavigateToSettings = {
                                showSettings = true
                            }
                        )
                    }
                    1 -> {
                        ReflectScreen(
                            onNavigateToSettings = {
                                showSettings = true
                            }
                        )
                    }
                }
            }
        }
    }
}
