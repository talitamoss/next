package com.domain.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.domain.app.core.plugin.Plugin

/**
 * Settings screen for managing app preferences and plugins
 * 
 * File location: app/src/main/java/com/domain/app/ui/settings/SettingsScreen.kt
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // Theme Section
            item {
                SettingsSection(title = "Appearance") {
                    SettingsItem(
                        icon = Icons.Default.Palette,
                        title = "Theme",
                        subtitle = uiState.currentTheme,
                        onClick = viewModel::showThemeDialog
                    )
                }
            }
            
            // Plugin Management Section
            item {
                SettingsSection(title = "Plugin Management") {
                    SettingsItem(
                        icon = Icons.Default.Extension,
                        title = "Manage Plugins",
                        subtitle = "${uiState.enabledPluginsCount} enabled",
                        onClick = viewModel::showPluginManagement
                    )
                }
            }
            
            // Data & Privacy Section
            item {
                SettingsSection(title = "Data & Privacy") {
                    SettingsItem(
                        icon = Icons.Default.Security,
                        title = "Security & Permissions",
                        subtitle = "Manage plugin permissions",
                        onClick = { /* TODO: Navigate to security */ }
                    )
                    
                    SettingsItem(
                        icon = Icons.Default.Backup,
                        title = "Backup & Restore",
                        subtitle = "Manage your data backups",
                        onClick = { /* TODO: Navigate to backup */ }
                    )
                    
                    SettingsItem(
                        icon = Icons.Default.CloudUpload,
                        title = "Export Data",
                        subtitle = "Export your data in various formats",
                        onClick = viewModel::showExportDialog
                    )
                    
                    SettingsItem(
                        icon = Icons.Default.DeleteForever,
                        title = "Clear All Data",
                        subtitle = "Permanently delete all app data",
                        onClick = viewModel::showClearDataDialog,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            // About Section
            item {
                SettingsSection(title = "About") {
                    SettingsItem(
                        icon = Icons.Default.Info,
                        title = "Version",
                        subtitle = "1.0.0"
                    )
                    
                    SettingsItem(
                        icon = Icons.Default.Code,
                        title = "Open Source Licenses",
                        subtitle = "View open source licenses",
                        onClick = { /* TODO: Show licenses */ }
                    )
                }
            }
        }
    }
    
    // Dialogs
    if (uiState.showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = uiState.currentTheme,
            onThemeSelected = viewModel::setTheme,
            onDismiss = viewModel::hideThemeDialog
        )
    }
    
    if (uiState.showPluginManagement) {
        PluginManagementDialog(
            plugins = uiState.allPlugins,
            enabledPlugins = uiState.enabledPluginIds,
            dashboardPlugins = uiState.dashboardPluginIds,
            onTogglePlugin = viewModel::togglePlugin,
            onToggleDashboard = viewModel::toggleDashboard,
            onNavigateToSecurity = { pluginId ->
                viewModel.hidePluginManagement()
                // TODO: Navigate to plugin security
            },
            onDismiss = viewModel::hidePluginManagement
        )
    }
    
    if (uiState.showExportDialog) {
        ExportDataDialog(
            onExportFormat = viewModel::exportData,
            onDismiss = viewModel::hideExportDialog
        )
    }
    
    if (uiState.showClearDataDialog) {
        AlertDialog(
            onDismissRequest = viewModel::hideClearDataDialog,
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Clear All Data?") },
            text = { 
                Text("This will permanently delete all your data, including all plugin data and preferences. This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel::clearAllData
                        viewModel::hideClearDataDialog
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::hideClearDataDialog) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Success/Error messages
    uiState.message?.let { message ->
        LaunchedEffect(message) {
            // Show snackbar or toast
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint
            )
        },
        modifier = if (onClick != null) {
            Modifier.clickable(onClick = onClick)
        } else {
            Modifier
        }
    )
}

@Composable
private fun ThemeSelectionDialog(
    currentTheme: String,
    onThemeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val themes = listOf("System", "Light", "Dark")
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Theme") },
        text = {
            Column {
                themes.forEach { theme ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onThemeSelected(theme)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = theme == currentTheme,
                            onClick = {
                                onThemeSelected(theme)
                                onDismiss()
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(theme)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun PluginManagementDialog(
    plugins: List<Plugin>,
    enabledPlugins: Set<String>,
    dashboardPlugins: Set<String>,
    onTogglePlugin: (String, Boolean) -> Unit,
    onToggleDashboard: (String, Boolean) -> Unit,
    onNavigateToSecurity: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Plugins") },
        text = {
            LazyColumn {
                items(plugins) { plugin ->
                    PluginManagementItem(
                        plugin = plugin,
                        isEnabled = enabledPlugins.contains(plugin.id),
                        isOnDashboard = dashboardPlugins.contains(plugin.id),
                        onToggleEnabled = { enabled ->
                            onTogglePlugin(plugin.id, enabled)
                        },
                        onToggleDashboard = { onDashboard ->
                            onToggleDashboard(plugin.id, onDashboard)
                        },
                        onSecurityClick = {
                            onNavigateToSecurity(plugin.id)
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
private fun PluginManagementItem(
    plugin: Plugin,
    isEnabled: Boolean,
    isOnDashboard: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onToggleDashboard: (Boolean) -> Unit,
    onSecurityClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = plugin.metadata.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = plugin.metadata.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggleEnabled
                )
            }
            
            if (isEnabled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isOnDashboard,
                            onCheckedChange = onToggleDashboard
                        )
                        Text(
                            text = "Show on dashboard",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    TextButton(onClick = onSecurityClick) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Permissions")
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportDataDialog(
    onExportFormat: (ExportFormat) -> Unit,
    onDismiss: () -> Unit
) {
    val formats = listOf(
        ExportFormat.CSV to "CSV (Comma-separated values)",
        ExportFormat.JSON to "JSON (JavaScript Object Notation)",
        ExportFormat.FHIR to "FHIR (Healthcare standard)"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Data") },
        text = {
            Column {
                Text(
                    "Choose export format:",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                formats.forEach { (format, description) ->
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                onExportFormat(format)
                                onDismiss()
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = format.name,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Export format enum
enum class ExportFormat {
    CSV, JSON, FHIR
}
