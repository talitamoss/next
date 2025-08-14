package com.domain.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { 
                        Icon(
                            imageVector = AppIcons.Navigation.home,
                            contentDescription = "Dashboard"
                        )
                    },
                    label = { Text("Dashboard") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { 
                        Icon(
                            imageVector = AppIcons.User.group,
                            contentDescription = "Social"
                        )
                    },
                    label = { Text("Social") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { 
                        Icon(
                            imageVector = AppIcons.Data.chart,
                            contentDescription = "Data"
                        )
                    },
                    label = { Text("Data") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { 
                        Icon(
                            imageVector = AppIcons.Navigation.settings,
                            contentDescription = "Settings"
                        )
                    },
                    label = { Text("Settings") }
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
                        // For now, just log or show a toast
                    },
                    onNavigateToSettings = {
                        selectedTab = 3
                    }
                )
                1 -> SocialPlaceholderScreen()
                2 -> DataScreen(
                    onNavigateBack = {
                        selectedTab = 0
                    }
                )
                3 -> SettingsScreen(
                    onNavigateBack = {
                        selectedTab = 0
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
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialPlaceholderScreen() {
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
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = AppIcons.User.group,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Social Features Coming Soon",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Connect and share with trusted friends",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
