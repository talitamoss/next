package com.domain.app.ui.settings

import androidx.compose.foundation.clickable
import com.domain.app.ui.utils.getPluginIcon
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
import com.domain.app.ui.security.PluginPermissionDialog

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
                ),
                actions = {
                    IconButton(
                        onClick = { navController.navigate("security_audit") }
                    ) {
                        Icon(AppIcons.Security.shield, contentDescription = "Security Audit")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // Plugin Management Section - Compressed to button
            item {
                SettingsSectionHeader(title = "Plugins")
            }
            
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    onClick = { navController.navigate("plugins") }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Manage Plugins",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "${uiState.plugins.size} plugins installed • ${uiState.pluginStates.values.count { it.isCollecting }} active",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Plugin icons preview (first 3 active plugins)
                            val activePlugins = uiState.plugins.filter { plugin ->
                                uiState.pluginStates[plugin.id]?.isCollecting == true
                            }.take(3)
                            
                            activePlugins.forEach { plugin ->
                                Icon(
                                    imageVector = AppIcons.getPluginIcon(plugin.id),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            
                            if (activePlugins.size < uiState.plugins.filter { plugin ->
                                uiState.pluginStates[plugin.id]?.isCollecting == true
                            }.size) {
                                Text(
                                    text = "+${uiState.plugins.filter { plugin ->
                                        uiState.pluginStates[plugin.id]?.isCollecting == true
                                    }.size - activePlugins.size}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Icon(
                                imageVector = AppIcons.Control.chevronRight,
                                contentDescription = "Open plugins",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
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
    
    // Navigate to plugin security if needed
    uiState.navigateToSecurity?.let { pluginId ->
        LaunchedEffect(pluginId) {
            navController.navigate("plugin_security/$pluginId")
            viewModel.clearNavigation()
        }
    }
    
    // Permission request dialog
    if (uiState.showPermissionRequest) {
        uiState.pendingPlugin?.let { plugin ->
            PluginPermissionDialog(
                plugin = plugin,
                requestedPermissions = plugin.securityManifest.requestedCapabilities,
                onGrant = { viewModel.grantPendingPermissions() },
                onDeny = { viewModel.denyPendingPermissions() }
            )
        }
    }
    
    // Clear data confirmation dialog
    if (uiState.showClearDataConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelClearData() },
            title = { Text("Clear All Data?") },
            text = {
                Text("This will permanently delete all your recorded data. This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmClearAllData() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelClearData() }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Show message if any
    uiState.message?.let { message ->
        LaunchedEffect(message) {
            kotlinx.coroutines.delay(2000)
            viewModel.dismissMessage()
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
