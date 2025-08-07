// app/src/main/java/com/domain/app/ui/settings/PluginsScreen.kt
package com.domain.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.domain.app.core.plugin.Plugin
import com.domain.app.ui.security.PluginPermissionDialog
import com.domain.app.ui.theme.AppIcons
import kotlinx.coroutines.launch

/**
 * Plugin management screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    
    // Permission dialog
    if (uiState.showPermissionRequest && uiState.pendingPlugin != null) {
        PluginPermissionDialog(
            plugin = uiState.pendingPlugin!!,
            onGrant = { viewModel.grantPendingPermissions() },
            onDeny = { viewModel.denyPendingPermissions() }
        )
    }
    
    // Message handling
    uiState.message?.let { message ->
        LaunchedEffect(message) {
            scope.launch {
                val snackbarHostState = SnackbarHostState()
                snackbarHostState.showSnackbar(message)
                viewModel.dismissMessage()
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Plugins") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
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
            // Plugin statistics card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
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
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${uiState.plugins.size} total plugins",
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
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
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
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
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
    
    // Error handling
    val snackbarHostState = remember { SnackbarHostState() }
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = error,
                    duration = SnackbarDuration.Short
                )
                viewModel.dismissError()
            }
        }
    }
}

/**
 * Individual plugin settings item
 */
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
            .padding(horizontal = 16.dp, vertical = 4.dp),
        onClick = onConfigure
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Extension, // Would use getPluginIcon(plugin) 
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = if (isEnabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = plugin.metadata.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = plugin.metadata.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        if (isOnDashboard) {
                            AssistChip(
                                onClick = onToggleDashboard,
                                label = { Text("On Dashboard") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Dashboard,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                        
                        if (plugin.securityManifest.requestedCapabilities.isNotEmpty()) {
                            AssistChip(
                                onClick = onSecuritySettings,
                                label = { 
                                    Text("${plugin.securityManifest.requestedCapabilities.size} permissions")
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Security,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }
            
            Switch(
                checked = isEnabled,
                onCheckedChange = { onToggleEnabled() }
            )
        }
    }
}
