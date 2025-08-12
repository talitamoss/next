// app/src/main/java/com/domain/app/core/plugin/security/PluginSecurityScreen.kt
package com.domain.app.core.plugin.security

import com.domain.app.core.plugin.getRiskLevel
import com.domain.app.core.plugin.getDescription
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.domain.app.core.plugin.Plugin
import com.domain.app.core.plugin.PluginCapability
import com.domain.app.core.plugin.RiskLevel
import com.domain.app.ui.theme.AppIcons

/**
 * Main security screen for managing plugin permissions
 */
@Composable
fun PluginSecurityScreen(
    plugins: List<Plugin>,
    onPluginClick: (Plugin) -> Unit,
    onRevokePermission: (Plugin, PluginCapability) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "Plugin Security",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        items(plugins) { plugin ->
            PluginSecurityCard(
                plugin = plugin,
                onClick = { onPluginClick(plugin) },
                onRevokePermission = { capability ->
                    onRevokePermission(plugin, capability)
                }
            )
        }
    }
}

@Composable
private fun PluginSecurityCard(
    plugin: Plugin,
    onClick: () -> Unit,
    onRevokePermission: (PluginCapability) -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = plugin.metadata.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${plugin.securityManifest.requestedCapabilities.size} permissions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Trust level indicator
                if (plugin.trustLevel != null) {
                    TrustLevelIndicator(plugin.trustLevel)
                }
            }
            
            // Show permissions if expanded
            if (plugin.securityManifest.requestedCapabilities.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                plugin.securityManifest.requestedCapabilities.forEach { capability ->
                    PermissionItem(
                        capability = capability,
                        onRevoke = { onRevokePermission(capability) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionItem(
    capability: PluginCapability,
    onRevoke: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getIconForCapability(capability),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
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
            horizontalArrangement = Arrangement.SpaceBetween,
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
            
            Text(
                text = when (trustLevel) {
                    PluginTrustLevel.OFFICIAL -> "Official Plugin"
                    PluginTrustLevel.VERIFIED -> "Verified"
                    PluginTrustLevel.COMMUNITY -> "Community"
                    PluginTrustLevel.UNTRUSTED -> "Untrusted"
                    PluginTrustLevel.BLOCKED -> "Blocked"
                    PluginTrustLevel.QUARANTINED -> "Under Review"
                },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// Helper function to get icon for capability
// Maps capabilities to appropriate icons from the app's icon set
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
        
        // Default icon for any capabilities not explicitly mapped
        else -> AppIcons.Plugin.custom
    }
}

// Note: The getDescription() and getRiskLevel() extension functions 
// are already defined in PluginCapability.kt and should not be duplicated here
