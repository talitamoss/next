package com.domain.app.ui.security

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.domain.app.core.plugin.PluginCapability
import com.domain.app.ui.theme.AppIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginSecurityScreen(
    pluginId: String,
    navController: NavController,
    viewModel: PluginSecurityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(pluginId) {
        viewModel.loadPlugin(pluginId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Plugin Security") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(AppIcons.Navigation.back, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.plugin != null) {
                        IconButton(
                            onClick = { viewModel.showSecurityInfo() }
                        ) {
                            Icon(AppIcons.Status.info, contentDescription = "Info")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        uiState.plugin?.let { plugin ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // Plugin header
                item {
                    PluginSecurityHeader(
                        plugin = plugin,
                        riskScore = uiState.riskScore
                    )
                }
                
                // Security summary
                item {
                    SecuritySummaryCard(
                        summary = uiState.securitySummary,
                        onViewHistory = { viewModel.showSecurityHistory() }
                    )
                }
                
                // Granted permissions
                item {
                    SettingsSectionHeader(title = "Granted Permissions")
                }
                
                if (uiState.grantedPermissions.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                text = "No permissions granted",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else {
                    items(uiState.grantedPermissions.toList()) { capability ->
                        PermissionSettingItem(
                            capability = capability,
                            isGranted = true,
                            onToggle = { viewModel.revokePermission(capability) }
                        )
                    }
                }
                
                // Available permissions
                val availablePermissions = plugin.securityManifest.requestedCapabilities - uiState.grantedPermissions
                if (availablePermissions.isNotEmpty()) {
                    item {
                        SettingsSectionHeader(title = "Available Permissions")
                    }
                    
                    items(availablePermissions.toList()) { capability ->
                        PermissionSettingItem(
                            capability = capability,
                            isGranted = false,
                            onToggle = { viewModel.requestPermission(capability) }
                        )
                    }
                }
                
                // Data access
                item {
                    SettingsSectionHeader(title = "Data Access")
                }
                
                item {
                    DataAccessCard(
                        plugin = plugin,
                        dataAccessCount = uiState.dataAccessCount
                    )
                }
                
                // Danger zone
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    SettingsSectionHeader(
                        title = "Danger Zone",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                
                item {
                    DangerZoneActions(
                        onRevokeAll = { viewModel.revokeAllPermissions() },
                        onDeleteData = { viewModel.deletePluginData() },
                        onBlockPlugin = { viewModel.blockPlugin() }
                    )
                }
            }
        } ?: run {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
    
    // Dialogs
    if (uiState.showPermissionDialog) {
        uiState.pendingPermission?.let { capability ->
            SimplePermissionDialog(
                capability = capability,
                onGrant = { viewModel.grantPendingPermission() },
                onDeny = { viewModel.dismissPermissionDialog() }
            )
        }
    }
    
    if (uiState.showSecurityHistory) {
        SecurityHistoryDialog(
            events = uiState.securityEvents,
            onDismiss = { viewModel.dismissSecurityHistory() }
        )
    }
}

@Composable
fun PluginSecurityHeader(
    plugin: Plugin,
    riskScore: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = AppIcons.getPluginIcon(plugin.id),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = plugin.metadata.name,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "by ${plugin.metadata.author}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Risk score badge
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = when {
                        riskScore < 20 -> MaterialTheme.colorScheme.primaryContainer
                        riskScore < 50 -> MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.errorContainer
                    }
                ) {
                    Text(
                        text = "Risk: $riskScore",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            
            // Trust level
            TrustLevelIndicator(plugin.trustLevel)
        }
    }
}

@Composable
fun SecuritySummaryCard(
    summary: com.domain.app.core.plugin.security.SecuritySummary?,
    onViewHistory: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Security Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            if (summary != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SummaryMetric("Events", summary.totalEvents)
                    SummaryMetric("Violations", summary.violations)
                    SummaryMetric("Data Access", summary.dataAccesses)
                }
                
                TextButton(
                    onClick = onViewHistory,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("View Security History")
                }
            } else {
                Text(
                    text = "No security events recorded",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun SummaryMetric(
    label: String,
    value: Int
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PermissionSettingItem(
    capability: PluginCapability,
    isGranted: Boolean,
    onToggle: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(capability.name.replace("_", " ").lowercase().capitalize())
        },
        supportingContent = {
            Text(capability.getDescription())
        },
        leadingContent = {
            Icon(
                imageVector = getIconForCapability(capability),
                contentDescription = null,
                tint = if (isGranted) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            Switch(
                checked = isGranted,
                onCheckedChange = { onToggle() }
            )
        }
    )
}

@Composable
fun DataAccessCard(
    plugin: Plugin,
    dataAccessCount: Map<String, Int>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Data Sensitivity: ${plugin.securityManifest.dataSensitivity}",
                style = MaterialTheme.typography.bodyMedium
            )
            
            if (dataAccessCount.isNotEmpty()) {
                Divider()
                dataAccessCount.forEach { (type, count) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = type.replace("_", " ").lowercase().capitalize(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DangerZoneActions(
    onRevokeAll: () -> Unit,
    onDeleteData: () -> Unit,
    onBlockPlugin: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onRevokeAll,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(AppIcons.Security.lock, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Revoke All Permissions")
            }
            
            OutlinedButton(
                onClick = onDeleteData,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(AppIcons.Action.delete, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete Plugin Data")
            }
            
            Button(
                onClick = onBlockPlugin,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(AppIcons.Action.delete, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Block Plugin")
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(
    title: String,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = color,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

private fun getIconForCapability(capability: PluginCapability): androidx.compose.ui.graphics.vector.ImageVector {
    return when (capability) {
        PluginCapability.COLLECT_DATA -> AppIcons.Action.add
        PluginCapability.READ_OWN_DATA,
        PluginCapability.READ_ALL_DATA -> AppIcons.Storage.folder
        PluginCapability.DELETE_DATA -> AppIcons.Action.delete
        PluginCapability.SHOW_NOTIFICATIONS -> AppIcons.Communication.notification
        PluginCapability.NETWORK_ACCESS -> AppIcons.Storage.cloud
        PluginCapability.ACCESS_LOCATION -> AppIcons.Plugin.location
        PluginCapability.LOCAL_STORAGE -> AppIcons.Storage.storage
        PluginCapability.EXPORT_DATA -> AppIcons.Data.upload
        PluginCapability.MODIFY_SETTINGS -> AppIcons.Navigation.settings
        else -> AppIcons.Plugin.custom
    }
}
