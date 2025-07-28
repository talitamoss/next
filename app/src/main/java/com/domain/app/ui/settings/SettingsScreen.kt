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
import com.domain.app.core.plugin.security.PluginTrustLevel
import com.domain.app.ui.security.PluginPermissionDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Handle navigation
    LaunchedEffect(uiState.navigateToSecurity) {
        uiState.navigateToSecurity?.let { pluginId ->
            navController.navigate("plugin_security/$pluginId")
            viewModel.clearNavigation()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        snackbarHost = {
            SnackbarHost { 
                uiState.message?.let { message ->
                    Snackbar(
                        snackbarData = object : SnackbarData {
                            override val visuals = SnackbarVisuals(
                                message = message,
                                duration = SnackbarDuration.Short
                            )
                            override fun performAction() { viewModel.dismissMessage() }
                            override fun dismiss() { viewModel.dismissMessage() }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // Plugin Management Section
            item {
                SettingsSectionHeader(title = "Plugin Management")
            }
            
            item {
                Text(
                    text = "Enable/disable plugins and manage permissions",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            
            // Show all plugins with enable/disable and security
            items(uiState.plugins) { plugin ->
                PluginManagementItem(
                    plugin = plugin,
                    isEnabled = uiState.pluginStates[plugin.id]?.isEnabled ?: false,
                    onToggle = { viewModel.togglePlugin(plugin.id) },
                    onSecurityClick = { viewModel.navigateToPluginSecurity(plugin.id) }
                )
            }
            
            item { Divider(modifier = Modifier.padding(vertical = 16.dp)) }
            
            // Dashboard Management Section
            item {
                SettingsSectionHeader(title = "Dashboard Plugins")
            }
            
            item {
                Text(
                    text = "Choose which plugins appear on your dashboard",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            
            // Show enabled plugins with dashboard toggle
            items(uiState.plugins.filter { plugin -> 
                uiState.pluginStates[plugin.id]?.isEnabled == true 
            }) { plugin ->
                DashboardPluginItem(
                    plugin = plugin,
                    isOnDashboard = uiState.dashboardPluginIds.contains(plugin.id),
                    onToggle = { viewModel.toggleDashboard(plugin.id) }
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
                    icon = AppIcons.Security.shield,
                    title = "Security Audit",
                    subtitle = "View plugin permissions and security events",
                    onClick = { navController.navigate("security_audit") }
                )
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
                    icon = AppIcons.Security.fingerprint,
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
    
    // Permission request dialog
    if (uiState.showPermissionRequest && uiState.pendingPlugin != null) {
        PluginPermissionDialog(
            plugin = uiState.pendingPlugin,
            requestedPermissions = uiState.pendingPlugin.securityManifest.requestedCapabilities,
            onGrant = { viewModel.grantPendingPermissions() },
            onDeny = { viewModel.denyPendingPermissions() }
        )
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
fun PluginManagementItem(
    plugin: Plugin,
    isEnabled: Boolean,
    onToggle: () -> Unit,
    onSecurityClick: () -> Unit
) {
    ListItem(
        headlineContent = { 
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(plugin.metadata.name)
                // Trust level badge
                when (plugin.trustLevel) {
                    PluginTrustLevel.OFFICIAL -> {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "Official",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    PluginTrustLevel.VERIFIED -> {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "Verified",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    else -> {}
                }
            }
        },
        supportingContent = { 
            Column {
                Text(plugin.metadata.description)
                Text(
                    text = "v${plugin.metadata.version} by ${plugin.metadata.author}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        leadingContent = {
            Icon(
                imageVector = AppIcons.getPluginIcon(plugin.id),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        },
        trailingContent = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Security button
                IconButton(onClick = onSecurityClick) {
                    Icon(
                        imageVector = AppIcons.Security.shield,
                        contentDescription = "Security settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Enable/disable switch
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { onToggle() }
                )
            }
        }
    )
}

@Composable
fun DashboardPluginItem(
    plugin: Plugin,
    isOnDashboard: Boolean,
    onToggle: () -> Unit
) {
    ListItem(
        headlineContent = { Text(plugin.metadata.name) },
        leadingContent = {
            Icon(
                imageVector = AppIcons.getPluginIcon(plugin.id),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        },
        trailingContent = {
            Switch(
                checked = isOnDashboard,
                onCheckedChange = { onToggle() }
            )
        }
    )
}
