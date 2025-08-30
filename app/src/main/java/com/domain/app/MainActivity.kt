// app/src/main/java/com/domain/app/MainActivity.kt
package com.domain.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.domain.app.ui.dashboard.DashboardScreen
import com.domain.app.ui.data.DataScreen
import com.domain.app.ui.settings.SettingsScreen
import com.domain.app.ui.theme.AppTheme
import com.domain.app.ui.theme.AppIcons
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
    var selectedTab by remember { mutableStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }
    
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
                        onClick = { selectedTab = 0 },
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
                        onClick = { selectedTab = 1 },
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (selectedTab) {
                    0 -> DashboardScreen(
                        onNavigateToPlugin = { plugin ->
                            // TODO: Navigate to plugin detail screen
                        },
                        onNavigateToSettings = {
                            showSettings = true
                        }
                    )
                    1 -> DataScreen(
                        onNavigateToSettings = {
                            showSettings = true
                        }
                    )
                }
            }
        }
    }
}
