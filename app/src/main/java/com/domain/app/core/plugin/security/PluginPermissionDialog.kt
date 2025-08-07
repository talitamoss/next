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
import com.domain.app.core.plugin.PluginTrustLevel
import com.domain.app.core.plugin.RiskLevel
import com.domain.app.core.plugin.RiskWarning
import com.domain.app.core.plugin.PluginTrustLevel
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
                    items(requestedPermissions.toList()) { capability ->
                        PermissionItem(capability)
                    }
                }
                
                // Info text
                Text(
                    text = "You can change these permissions later in Settings.",
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
                    containerColor = when (plugin.trustLevel) {
                        PluginTrustLevel.OFFICIAL,
                        PluginTrustLevel.VERIFIED -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.error
                    }
                )
            ) {
                Text("Grant Permissions")
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
    val riskLevel = capability.getRiskLevel()
    val riskColor = when (riskLevel) {
        RiskLevel.LOW -> MaterialTheme.colorScheme.primary
        RiskLevel.MEDIUM -> MaterialTheme.colorScheme.tertiary
        RiskLevel.HIGH -> MaterialTheme.colorScheme.error
        RiskLevel.CRITICAL -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                MaterialTheme.shapes.small
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = getIconForCapability(capability),
            contentDescription = null,
            tint = riskColor,
            modifier = Modifier.size(24.dp)
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
        
        Surface(
            shape = MaterialTheme.shapes.small,
            color = riskColor.copy(alpha = 0.1f)
        ) {
            Text(
                text = riskLevel.name,
                style = MaterialTheme.typography.labelSmall,
                color = riskColor,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun TrustLevelIndicator(trustLevel: PluginTrustLevel) {
    val backgroundColor = when (trustLevel) {
        PluginTrustLevel.OFFICIAL -> MaterialTheme.colorScheme.primaryContainer
        PluginTrustLevel.VERIFIED -> MaterialTheme.colorScheme.secondaryContainer
        PluginTrustLevel.COMMUNITY -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.errorContainer
    }
    
    Surface(
        shape = MaterialTheme.shapes.small,
        color = backgroundColor
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
        else -> AppIcons.Plugin.custom  // Fixed: Using else instead of UNKNOWN
    }
}

// Extension functions for PluginCapability with exhaustive when expressions
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
        PluginCapability.BACKGROUND_PROCESS -> "Run background processes"
        PluginCapability.BACKGROUND_SYNC -> "Sync in background"
        PluginCapability.MODIFY_DATA -> "Modify existing data"
        PluginCapability.CUSTOM_UI -> "Display custom UI elements"
        PluginCapability.MODIFY_THEME -> "Modify app theme"
        PluginCapability.ADD_MENU_ITEMS -> "Add menu items"
        PluginCapability.FULLSCREEN_UI -> "Use fullscreen mode"
        PluginCapability.FILE_ACCESS -> "Access files"
        PluginCapability.CAMERA_ACCESS -> "Access camera"
        PluginCapability.MICROPHONE_ACCESS -> "Access microphone"
        PluginCapability.SHARE_DATA -> "Share data"
        PluginCapability.IMPORT_DATA -> "Import data"
        PluginCapability.INTEGRATE_SERVICES -> "Integrate with services"
        PluginCapability.ACCESS_SENSORS -> "Access device sensors"
        PluginCapability.ACCESS_BIOMETRIC -> "Access biometric data"
        PluginCapability.INSTALL_PLUGINS -> "Install other plugins"
        PluginCapability.ANALYTICS_BASIC -> "Basic analytics"
        PluginCapability.ANALYTICS_DETAILED -> "Detailed analytics"
        PluginCapability.SEND_EMAILS -> "Send emails"
        PluginCapability.SEND_SMS -> "Send SMS messages"
        PluginCapability.PUSH_NOTIFICATIONS -> "Send push notifications"
        PluginCapability.CLOUD_STORAGE -> "Use cloud storage"
        PluginCapability.CACHE_DATA -> "Cache data"
        else -> "Unknown capability"  // Fixed: Using else for exhaustive when
    }
}

fun PluginCapability.getRiskLevel(): RiskLevel {
    return when (this) {
        PluginCapability.COLLECT_DATA,
        PluginCapability.READ_OWN_DATA,
        PluginCapability.SHOW_NOTIFICATIONS,
        PluginCapability.LOCAL_STORAGE,
        PluginCapability.CACHE_DATA,
        PluginCapability.CUSTOM_UI,
        PluginCapability.ADD_MENU_ITEMS -> RiskLevel.LOW
        
        PluginCapability.EXPORT_DATA,
        PluginCapability.SCHEDULE_NOTIFICATIONS,
        PluginCapability.MODIFY_SETTINGS,
        PluginCapability.BACKGROUND_SYNC,
        PluginCapability.SHARE_DATA,
        PluginCapability.ANALYTICS_BASIC,
        PluginCapability.IMPORT_DATA -> RiskLevel.MEDIUM
        
        PluginCapability.READ_ALL_DATA,
        PluginCapability.DELETE_DATA,
        PluginCapability.NETWORK_ACCESS,
        PluginCapability.SYSTEM_NOTIFICATIONS,
        PluginCapability.EXTERNAL_STORAGE,
        PluginCapability.ACCESS_LOCATION,
        PluginCapability.BACKGROUND_PROCESSING,
        PluginCapability.BACKGROUND_PROCESS,
        PluginCapability.FILE_ACCESS,
        PluginCapability.INTEGRATE_SERVICES,
        PluginCapability.ANALYTICS_DETAILED,
        PluginCapability.ACCESS_SENSORS -> RiskLevel.HIGH
        
        PluginCapability.CLOUD_SYNC,
        PluginCapability.MODIFY_DATA,
        PluginCapability.MODIFY_THEME,
        PluginCapability.FULLSCREEN_UI,
        PluginCapability.CAMERA_ACCESS,
        PluginCapability.MICROPHONE_ACCESS,
        PluginCapability.ACCESS_BIOMETRIC,
        PluginCapability.INSTALL_PLUGINS,
        PluginCapability.SEND_EMAILS,
        PluginCapability.SEND_SMS,
        PluginCapability.PUSH_NOTIFICATIONS,
        PluginCapability.CLOUD_STORAGE -> RiskLevel.CRITICAL
        
        else -> RiskLevel.UNKNOWN  // Fixed: Using else instead of UNKNOWN case
    }
}
