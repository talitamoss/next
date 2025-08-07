package com.domain.app.core.plugin.security

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.domain.app.core.plugin.Plugin
import com.domain.app.core.plugin.PluginCapability
import com.domain.app.core.plugin.RiskLevel
import com.domain.app.core.plugin.RiskWarning
import com.domain.app.ui.theme.AppIcons
import com.domain.app.ui.utils.getPluginIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginPermissionDialog(
    plugin: Plugin,
    requestedPermissions: Set<PluginCapability>,
    riskWarnings: List<RiskWarning> = emptyList(),
    onGrant: () -> Unit,
    onDeny: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDeny,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Plugin icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getPluginIcon(plugin),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Column {
                    Text(
                        text = plugin.metadata.name,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Permission Request",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Trust level indicator
                TrustLevelIndicator(plugin.trustLevel)
                
                // Risk warnings if any
                if (riskWarnings.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = AppIcons.Status.warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Security Warnings",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            riskWarnings.forEach { warning ->
                                Text(
                                    text = "• ${warning.message}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
                
                // Permissions list
                Text(
                    text = "This plugin requests the following permissions:",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(requestedPermissions.toList()) { permission ->
                        PermissionItem(permission)
                    }
                }
                
                // Additional info
                Text(
                    text = "You can modify these permissions anytime in Settings",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onGrant,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (riskWarnings.isEmpty()) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.error
                )
            ) {
                Text(if (riskWarnings.isEmpty()) "Grant Permissions" else "Grant Anyway")
            }
        },
        dismissButton = {
            TextButton(onClick = onDeny) {
                Text("Deny")
            }
        }
    )
}

@Composable
private fun PermissionItem(capability: PluginCapability) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (capability.getRiskLevel()) {
                RiskLevel.LOW -> MaterialTheme.colorScheme.surfaceVariant
                RiskLevel.MEDIUM -> MaterialTheme.colorScheme.tertiaryContainer
                RiskLevel.HIGH -> MaterialTheme.colorScheme.errorContainer
                RiskLevel.CRITICAL -> MaterialTheme.colorScheme.errorContainer
                RiskLevel.UNKNOWN -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getIconForCapability(capability),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = when (capability.getRiskLevel()) {
                    RiskLevel.HIGH, RiskLevel.CRITICAL -> MaterialTheme.colorScheme.error
                    RiskLevel.MEDIUM -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = capability.name.replace("_", " ").lowercase()
                        .replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = capability.getDescription(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Risk indicator
            Text(
                text = when (capability.getRiskLevel()) {
                    RiskLevel.LOW -> "Low"
                    RiskLevel.MEDIUM -> "Medium"  
                    RiskLevel.HIGH -> "High"
                    RiskLevel.CRITICAL -> "Critical"
                    RiskLevel.UNKNOWN -> "Unknown"
                    else -> ""
                },
                style = MaterialTheme.typography.labelSmall,
                color = when (capability.getRiskLevel()) {
                    RiskLevel.HIGH, RiskLevel.CRITICAL -> MaterialTheme.colorScheme.error
                    RiskLevel.MEDIUM -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun TrustLevelIndicator(trustLevel: PluginTrustLevel) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = when (trustLevel) {
                PluginTrustLevel.OFFICIAL -> MaterialTheme.colorScheme.primaryContainer
                PluginTrustLevel.VERIFIED -> MaterialTheme.colorScheme.secondaryContainer
                PluginTrustLevel.COMMUNITY -> MaterialTheme.colorScheme.surfaceVariant
                PluginTrustLevel.UNTRUSTED -> MaterialTheme.colorScheme.errorContainer
                PluginTrustLevel.BLOCKED -> MaterialTheme.colorScheme.errorContainer
                PluginTrustLevel.QUARANTINED -> MaterialTheme.colorScheme.errorContainer
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (trustLevel) {
                    PluginTrustLevel.OFFICIAL -> AppIcons.Status.success
                    PluginTrustLevel.VERIFIED -> AppIcons.Security.shield
                    else -> AppIcons.Status.warning
                },
                contentDescription = null,
                tint = when (trustLevel) {
                    PluginTrustLevel.OFFICIAL -> MaterialTheme.colorScheme.onPrimaryContainer
                    PluginTrustLevel.VERIFIED -> MaterialTheme.colorScheme.onSecondaryContainer
                    else -> MaterialTheme.colorScheme.onErrorContainer
                }
            )
            Text(
                text = when (trustLevel) {
                    PluginTrustLevel.OFFICIAL -> "Official Plugin"
                    PluginTrustLevel.VERIFIED -> "Verified Plugin"
                    PluginTrustLevel.COMMUNITY -> "Community Plugin"
                    PluginTrustLevel.UNTRUSTED -> "Untrusted Plugin"
                    PluginTrustLevel.BLOCKED -> "Blocked Plugin"
                    PluginTrustLevel.QUARANTINED -> "Quarantined Plugin"
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// Helper function to get icon for capability
private fun getIconForCapability(capability: PluginCapability): androidx.compose.ui.graphics.vector.ImageVector {
    return when (capability) {
        PluginCapability.COLLECT_DATA -> AppIcons.Action.add
        PluginCapability.READ_OWN_DATA -> AppIcons.Storage.folder
        PluginCapability.READ_ALL_DATA -> AppIcons.Storage.database
        PluginCapability.DELETE_DATA -> AppIcons.Action.delete
        PluginCapability.EXPORT_DATA -> AppIcons.Data.upload
        PluginCapability.SHOW_NOTIFICATIONS -> AppIcons.Communication.notifications
        PluginCapability.SYSTEM_NOTIFICATIONS -> AppIcons.Communication.notifications
        PluginCapability.SCHEDULE_NOTIFICATIONS -> AppIcons.Data.calendar
        PluginCapability.LOCAL_STORAGE -> AppIcons.Storage.storage
        PluginCapability.EXTERNAL_STORAGE -> AppIcons.Storage.folder
        PluginCapability.CLOUD_SYNC -> AppIcons.Storage.cloud
        PluginCapability.NETWORK_ACCESS -> AppIcons.Communication.cloud
        PluginCapability.ACCESS_LOCATION -> AppIcons.Plugin.location
        PluginCapability.MODIFY_SETTINGS -> AppIcons.Navigation.settings
        PluginCapability.BACKGROUND_PROCESSING -> AppIcons.Status.sync
        PluginCapability.UNKNOWN -> AppIcons.Plugin.custom
    }
}

// Extension functions for PluginCapability
fun PluginCapability.getDescription(): String {
    return when (this) {
        PluginCapability.COLLECT_DATA -> "Collect and save behavioral data"
        PluginCapability.READ_OWN_DATA -> "Read data collected by this plugin"
        PluginCapability.READ_ALL_DATA -> "Read data from all plugins"
        PluginCapability.DELETE_DATA -> "Delete existing data points"
        PluginCapability.EXPORT_DATA -> "Export data to external formats"
        PluginCapability.SHOW_NOTIFICATIONS -> "Show in-app notifications"
        PluginCapability.SYSTEM_NOTIFICATIONS -> "Show system notifications"
        PluginCapability.SCHEDULE_NOTIFICATIONS -> "Schedule future notifications"
        PluginCapability.LOCAL_STORAGE -> "Store data locally on device"
        PluginCapability.EXTERNAL_STORAGE -> "Access external storage"
        PluginCapability.CLOUD_SYNC -> "Sync data with cloud services"
        PluginCapability.NETWORK_ACCESS -> "Access network resources"
        PluginCapability.ACCESS_LOCATION -> "Access device location"
        PluginCapability.MODIFY_SETTINGS -> "Modify app settings"
        PluginCapability.BACKGROUND_PROCESSING -> "Run background tasks"
        PluginCapability.UNKNOWN -> "Unknown capability"
    }
}

fun PluginCapability.getRiskLevel(): RiskLevel {
    return when (this) {
        PluginCapability.COLLECT_DATA,
        PluginCapability.READ_OWN_DATA,
        PluginCapability.SHOW_NOTIFICATIONS -> RiskLevel.LOW
        
        PluginCapability.LOCAL_STORAGE,
        PluginCapability.EXPORT_DATA,
        PluginCapability.SCHEDULE_NOTIFICATIONS,
        PluginCapability.MODIFY_SETTINGS -> RiskLevel.MEDIUM
        
        PluginCapability.READ_ALL_DATA,
        PluginCapability.DELETE_DATA,
        PluginCapability.NETWORK_ACCESS,
        PluginCapability.SYSTEM_NOTIFICATIONS,
        PluginCapability.EXTERNAL_STORAGE,
        PluginCapability.ACCESS_LOCATION -> RiskLevel.HIGH
        
        PluginCapability.CLOUD_SYNC,
        PluginCapability.BACKGROUND_PROCESSING -> RiskLevel.CRITICAL
        
        PluginCapability.UNKNOWN -> RiskLevel.UNKNOWN
    }
}
