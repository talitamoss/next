// app/src/main/java/com/domain/app/core/plugin/security/PluginPermissionDialog.kt
package com.domain.app.core.plugin.security

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.domain.app.core.plugin.Plugin
import com.domain.app.core.plugin.PluginCapability
import com.domain.app.core.plugin.RiskLevel
import com.domain.app.core.plugin.RiskWarning
import com.domain.app.ui.theme.AppIcons

/**
 * Dialog for requesting plugin permissions with detailed explanations
 */
@Composable
fun PluginPermissionDialog(
    plugin: Plugin,
    requestedPermissions: Set<PluginCapability>,
    riskWarnings: List<RiskWarning> = emptyList(),
    onGrant: () -> Unit,
    onDeny: () -> Unit
) {
    Dialog(
        onDismissRequest = onDeny,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header
                Text(
                    text = "Permission Request",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Plugin info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = AppIcons.Plugin.custom,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = plugin.manifest.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Version ${plugin.manifest.version}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Trust level if available
                plugin.trustLevel?.let { trustLevel ->
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
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (trustLevel) {
                                    PluginTrustLevel.OFFICIAL -> AppIcons.Status.verified
                                    PluginTrustLevel.VERIFIED -> AppIcons.Status.check
                                    PluginTrustLevel.COMMUNITY -> AppIcons.Status.info
                                    PluginTrustLevel.UNTRUSTED -> AppIcons.Status.warning
                                    PluginTrustLevel.BLOCKED -> AppIcons.Status.error
                                    PluginTrustLevel.QUARANTINED -> AppIcons.Status.pause
                                },
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (trustLevel) {
                                    PluginTrustLevel.OFFICIAL -> "Official Plugin"
                                    PluginTrustLevel.VERIFIED -> "Verified Plugin"
                                    PluginTrustLevel.COMMUNITY -> "Community Plugin"
                                    PluginTrustLevel.UNTRUSTED -> "Untrusted Source"
                                    PluginTrustLevel.BLOCKED -> "Blocked Plugin"
                                    PluginTrustLevel.QUARANTINED -> "Under Review"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Risk warnings
                if (riskWarnings.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = AppIcons.Status.warning,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Security Warnings",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            riskWarnings.forEach { warning ->
                                Text(
                                    text = "• ${warning.message}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Permissions list
                Text(
                    text = "This plugin requests the following permissions:",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(requestedPermissions.toList()) { capability ->
                        PermissionRequestItem(capability = capability)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDeny) {
                        Text("Deny")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onGrant) {
                        Text("Grant Permissions")
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRequestItem(capability: PluginCapability) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
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
            
            Spacer(modifier = Modifier.width(12.dp))
            
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
                
                // Show risk level for high-risk permissions
                if (capability.getRiskLevel() in listOf(RiskLevel.HIGH, RiskLevel.CRITICAL)) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "⚠️ ${capability.getRiskLevel().name} RISK",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
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
        PluginCapability.MODIFY_DATA -> AppIcons.Action.edit
        
        PluginCapability.EXPORT_DATA -> AppIcons.Data.upload
        PluginCapability.IMPORT_DATA -> AppIcons.Data.download
        PluginCapability.SHARE_DATA -> AppIcons.Communication.share
        PluginCapability.INTEGRATE_SERVICES -> AppIcons.Communication.link
        
        PluginCapability.SHOW_NOTIFICATIONS -> AppIcons.Communication.notifications
        PluginCapability.SYSTEM_NOTIFICATIONS -> AppIcons.Communication.notifications
        PluginCapability.SCHEDULE_NOTIFICATIONS -> AppIcons.Data.calendar
        PluginCapability.PUSH_NOTIFICATIONS -> AppIcons.Communication.notifications
        
        PluginCapability.LOCAL_STORAGE -> AppIcons.Storage.storage
        PluginCapability.EXTERNAL_STORAGE -> AppIcons.Storage.folder
        PluginCapability.CLOUD_STORAGE -> AppIcons.Storage.cloud
        PluginCapability.CLOUD_SYNC -> AppIcons.Storage.cloud
        PluginCapability.CACHE_DATA -> AppIcons.Storage.storage
        
        PluginCapability.NETWORK_ACCESS -> AppIcons.Communication.cloud
        PluginCapability.FILE_ACCESS -> AppIcons.Storage.folder
        PluginCapability.CAMERA_ACCESS -> AppIcons.Device.camera
        PluginCapability.MICROPHONE_ACCESS -> AppIcons.Device.mic
        
        PluginCapability.ACCESS_LOCATION -> AppIcons.Plugin.location
        PluginCapability.ACCESS_SENSORS -> AppIcons.Device.sensors
        PluginCapability.ACCESS_BIOMETRIC -> AppIcons.Device.fingerprint
        
        PluginCapability.MODIFY_SETTINGS -> AppIcons.Navigation.settings
        PluginCapability.INSTALL_PLUGINS -> AppIcons.Plugin.custom
        PluginCapability.MODIFY_THEME -> AppIcons.Action.palette
        PluginCapability.ADD_MENU_ITEMS -> AppIcons.Navigation.menu
        PluginCapability.CUSTOM_UI -> AppIcons.Action.dashboard
        PluginCapability.FULLSCREEN_UI -> AppIcons.Action.fullscreen
        
        PluginCapability.BACKGROUND_SYNC -> AppIcons.Status.sync
        PluginCapability.BACKGROUND_PROCESS -> AppIcons.Status.sync
        PluginCapability.BACKGROUND_PROCESSING -> AppIcons.Status.sync
        
        PluginCapability.ANALYTICS_BASIC -> AppIcons.Data.analytics
        PluginCapability.ANALYTICS_DETAILED -> AppIcons.Data.analytics
        
        PluginCapability.SEND_EMAILS -> AppIcons.Communication.email
        PluginCapability.SEND_SMS -> AppIcons.Communication.message
        
        // Default for any unmapped capabilities
        else -> AppIcons.Plugin.custom
    }
}

// Note: The getDescription() and getRiskLevel() extension functions 
// are already defined in PluginCapability.kt and should not be duplicated here
