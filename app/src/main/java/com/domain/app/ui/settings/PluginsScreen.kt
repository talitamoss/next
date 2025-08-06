// app/src/main/java/com/domain/app/ui/settings/PluginsScreen.kt
package com.domain.app.ui.settings

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.domain.app.core.plugin.Plugin
import com.domain.app.ui.theme.AppIcons
import com.domain.app.ui.utils.getPluginIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Handle permission dialog
    if (uiState.showPermissionRequest && uiState.pendingPlugin != null) {
        PluginPermissionDialog(
            plugin = uiState.pendingPlugin!!,
            onGrant = { viewModel.grantPendingPermissions() },
            onDeny = { viewModel.denyPendingPermissions() }
        )
    }
    
    // Handle messages
    uiState.message?.let { message ->
        LaunchedEffect(message) {
            // Auto-dismiss after 3 seconds
            delay(3000)
            viewModel.dismissMessage()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Plugin Management") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = AppIcons.Navigation.back,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Add plugin store */ }) {
                        Icon(
                            imageVector = AppIcons.Action.add,
                            contentDescription = "Add Plugin"
                        )
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = remember { SnackbarHostState() }
            ) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = if (uiState.error != null) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    }
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            // Plugin Statistics Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Plugin Overview",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${uiState.plugins.size} plugins available",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "${uiState.pluginStates.count { it.value.isCollecting }} active",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                        Icon(
                            imageVector = AppIcons.Plugin.custom,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            
            // Dashboard Plugins Section
            if (uiState.dashboardPluginIds.isNotEmpty()) {
                item {
                    Text(
                        text = "Dashboard Plugins",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                items(
                    items = uiState.plugins.filter { it.id in uiState.dashboardPluginIds },
                    key = { it.id }
                ) { plugin ->
                    PluginSettingsItem(
                        plugin = plugin,
                        isEnabled = uiState.pluginStates[plugin.id]?.isCollecting ?: false,
                        isOnDashboard = true,
                        onToggleEnabled = { viewModel.togglePlugin(plugin.id) },
                        onToggleDashboard = { viewModel.toggleDashboard(plugin.id) },
                        onConfigure = { 
                            navController.navigate("plugin_detail/${plugin.id}")
                        },
                        onSecuritySettings = {
                            navController.navigate("plugin_security/${plugin.id}")
                        }
                    )
                }
            }
            
            // All Plugins Section
            item {
                Text(
                    text = "All Plugins",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            items(
                items = uiState.plugins.filter { it.id !in uiState.dashboardPluginIds },
                key = { it.id }
            ) { plugin ->
                PluginSettingsItem(
                    plugin = plugin,
                    isEnabled = uiState.pluginStates[plugin.id]?.isCollecting ?: false,
                    isOnDashboard = false,
                    onToggleEnabled = { viewModel.togglePlugin(plugin.id) },
                    onToggleDashboard = { viewModel.toggleDashboard(plugin.id) },
                    onConfigure = { 
                        navController.navigate("plugin_detail/${plugin.id}")
                    },
                    onSecuritySettings = {
                        navController.navigate("plugin_security/${plugin.id}")
                    }
                )
            }
        }
    }
    
    // Show snackbar for messages
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.message, uiState.error) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            viewModel.dismissMessage()
        }
        uiState.error?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            viewModel.dismissError()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PluginSettingsItem(
    plugin: Plugin,
    isEnabled: Boolean,
    isOnDashboard: Boolean,
    onToggleEnabled: () -> Unit,
    onToggleDashboard: () -> Unit,
    onConfigure: () -> Unit,
    onSecuritySettings: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Plugin Icon and Info
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Plugin Icon
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (isEnabled) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getPluginIcon(plugin),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = if (isEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    
                    // Plugin Name and Description
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = plugin.metadata.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = plugin.metadata.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }
                }
                
                // Enable/Disable Switch
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { onToggleEnabled() }
                )
            }
            
            // Action Buttons
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Dashboard Toggle
                FilterChip(
                    selected = isOnDashboard,
                    onClick = onToggleDashboard,
                    label = { 
                        Text(
                            text = if (isOnDashboard) "On Dashboard" else "Add to Dashboard"
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (isOnDashboard) {
                                AppIcons.Action.check
                            } else {
                                AppIcons.Navigation.dashboard
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
                
                // Configure Button
                AssistChip(
                    onClick = onConfigure,
                    label = { Text("Configure") },
                    leadingIcon = {
                        Icon(
                            imageVector = AppIcons.Navigation.settings,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
                
                // Security Button
                AssistChip(
                    onClick = onSecuritySettings,
                    label = { Text("Security") },
                    leadingIcon = {
                        Icon(
                            imageVector = AppIcons.Security.shield,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
            
            // Plugin Stats (if enabled)
            if (isEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // TODO: Add actual stats from plugin
                    PluginStat(label = "Data Points", value = "0")
                    PluginStat(label = "Last Entry", value = "Never")
                    PluginStat(label = "Version", value = plugin.metadata.version)
                }
            }
        }
    }
}

@Composable
private fun PluginStat(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PluginPermissionDialog(
    plugin: Plugin,
    onGrant: () -> Unit,
    onDeny: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDeny,
        icon = {
            Icon(
                imageVector = AppIcons.Security.shield,
                contentDescription = null
            )
        },
        title = {
            Text("Permission Request")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "${plugin.metadata.name} requires the following permissions:",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        plugin.securityManifest.requestedCapabilities.forEach { capability ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = AppIcons.Action.check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = capability.name.replace("_", " ").lowercase()
                                        .replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
                
                Text(
                    text = "Grant these permissions to enable the plugin?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onGrant) {
                Text("Grant")
            }
        },
        dismissButton = {
            TextButton(onClick = onDeny) {
                Text("Deny")
            }
        }
    )
}
