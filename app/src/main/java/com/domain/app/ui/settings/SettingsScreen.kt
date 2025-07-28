package com.domain.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.domain.app.ui.theme.AppIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.domain.app.core.plugin.Plugin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // Dashboard Management Section
            item {
                SettingsSectionHeader(title = "Dashboard Layout")
            }
            
            item {
                DashboardManagementItem(
                    dashboardCount = uiState.dashboardPluginIds.size,
                    onClick = { /* TODO: Navigate to dashboard settings */ }
                )
            }
            
            item { Divider(modifier = Modifier.padding(vertical = 16.dp)) }
            
            // Plugin Management Section
            item {
                SettingsSectionHeader(title = "Plugins")
            }
            
            items(uiState.plugins) { plugin ->
                PluginSettingsItem(
                    plugin = plugin,
                    isEnabled = uiState.pluginStates[plugin.id]?.isEnabled ?: false,
                    isCollecting = uiState.pluginStates[plugin.id]?.isCollecting ?: false,
                    onToggle = { viewModel.togglePlugin(plugin.id) }
                )
            }
            
            item { Divider(modifier = Modifier.padding(vertical = 16.dp)) }
            
            // Data Management Section
            item {
                SettingsSectionHeader(title = "Data Management")
            }
            
            item {
                SettingsItem(
                    icon = AppIcons.Storage.cloudUpload,
                    title = "Export Data",
                    subtitle = "Export your data in CSV format",
                    onClick = { viewModel.exportData() }
                )
            }
            
            item {
                SettingsItem(
                    icon = AppIcons.Storage.cloudDownload,
                    title = "Import Data",
                    subtitle = "Import data from a backup",
                    onClick = { viewModel.importData() }
                )
            }
            
            item {
                SettingsItem(
                    icon = AppIcons.Action.delete,
                    title = "Clear All Data",
                    subtitle = "Delete all recorded data",
                    onClick = { viewModel.clearAllData() }
                )
            }
            
            item { Divider(modifier = Modifier.padding(vertical = 16.dp)) }
            
            // Security Section
            item {
                SettingsSectionHeader(title = "Security")
            }
            
            item {
                SettingsItem(
                    icon = AppIcons.Security.lock,
                    title = "Change PIN",
                    subtitle = "Update your security PIN",
                    onClick = { /* TODO: Navigate to PIN change */ }
                )
            }
            
            item {
                SettingsItem(
                    icon = AppIcons.Security.shield,
                    title = "Biometric Lock",
                    subtitle = "Use fingerprint or face unlock",
                    onClick = { /* TODO: Toggle biometric */ }
                )
            }
            
            item { Divider(modifier = Modifier.padding(vertical = 16.dp)) }
            
            // About Section
            item {
                SettingsSectionHeader(title = "About")
            }
            
            item {
                SettingsItem(
                    icon = AppIcons.Status.info,
                    title = "Version",
                    subtitle = "1.0.0",
                    onClick = { }
                )
            }
            
            item {
                SettingsItem(
                    icon = AppIcons.Action.more,
                    title = "Open Source",
                    subtitle = "View source code on GitHub",
                    onClick = { /* TODO: Open GitHub */ }
                )
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(
    title: String
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = Modifier.clickable { onClick() }
    )
}

@Composable
fun PluginSettingsItem(
    plugin: Plugin,
    isEnabled: Boolean,
    isCollecting: Boolean,
    onToggle: () -> Unit
) {
    ListItem(
        headlineContent = { Text(plugin.metadata.name) },
        supportingContent = { 
            Column {
                Text(plugin.metadata.description)
                if (isCollecting) {
                    Text(
                        text = "Collecting data",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        leadingContent = {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = plugin.metadata.name.first().toString(),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        },
        trailingContent = {
            Switch(
                checked = isEnabled,
                onCheckedChange = { onToggle() }
            )
        }
    )
}

@Composable
fun DashboardManagementItem(
    dashboardCount: Int,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text("Dashboard Plugins") },
        supportingContent = { 
            Text("$dashboardCount of 6 plugins shown on dashboard") 
        },
        leadingContent = {
            Icon(
                imageVector = AppIcons.Navigation.dashboard,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            Icon(
                imageVector = AppIcons.Control.chevronRight,
                contentDescription = null
            )
        },
        modifier = Modifier.clickable { onClick() }
    )
}
