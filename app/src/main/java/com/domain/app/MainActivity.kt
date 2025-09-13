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
import com.domain.app.ui.reflect.ReflectScreen
import com.domain.app.ui.settings.navigation.SettingsNavigation
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
    // Pager state for swipe navigation (2 screens: Collect and Reflect)
    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()
    var showSettings by remember { mutableStateOf(false) }
    
    if (showSettings) {
        // Settings navigation remains the same
        SettingsNavigation(
            onNavigateBack = {
                showSettings = false
            }
        )
    } else {
        Scaffold(
            bottomBar = {
                // Replace NavigationBar with TabRow for swipe sync
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Collect tab
                    Tab(
                        selected = pagerState.currentPage == 0,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(0)
                            }
                        },
                        text = { Text("Collect") },
                        icon = {
                            Icon(
                                imageVector = AppIcons.Navigation.home,
                                contentDescription = "Collect"
                            )
                        }
                    )
                    
                    // Reflect tab
                    Tab(
                        selected = pagerState.currentPage == 1,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(1)
                            }
                        },
                        text = { Text("Reflect") },
                        icon = {
                            Icon(
                                imageVector = AppIcons.Data.chart,
                                contentDescription = "Reflect"
                            )
                        }
                    )
                }
            }
        ) { paddingValues ->
            // HorizontalPager for swipe navigation
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) { page ->
                when (page) {
                    0 -> DashboardScreen(
                        onNavigateToPlugin = { plugin ->
                            // TODO: Navigate to plugin detail screen
                        },
                        onNavigateToSettings = {
                            showSettings = true
                        }
                    )
                    1 -> ReflectScreen(
                        onNavigateToSettings = {
                            showSettings = true
                        }
                    )
                }
            }
        }
    }
}
