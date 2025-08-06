package com.domain.app.ui.security

import androidx.compose.foundation.background
import com.domain.app.ui.utils.getPluginIcon
import com.domain.app.ui.utils.notification
import com.domain.app.ui.utils.storage
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
import com.domain.app.core.plugin.getRiskLevel
import com.domain.app.core.plugin.getDescription
import com.domain.app.core.plugin.RiskLevel
import com.domain.app.core.plugin.RiskWarning
import com.domain.app.core.plugin.security.PluginTrustLevel
import com.domain.app.ui.theme.AppIcons

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
                        imageVector = AppIcons.getPluginIcon(plugin.id),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Column {
                    Text(
                        text = "${plugin.metadata.name} Permissions",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Version ${plugin.metadata.version}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Trust level indicator
                item {
                    PluginTrustLevelCard(plugin.trustLevel)
                }
                
                // Plugin description
                item {
                    Text(
                        text = plugin.metadata.description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                // Risk warnings if any
                if (riskWarnings.isNotEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
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
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = "High Risk Permissions",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                                Text(
                                    text = "This plugin requests sensitive permissions. Grant only if you trust the developer.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
                
                // Permissions list
                item {
                    Text(
                        text = "This plugin requests:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                items(requestedPermissions.toList()) { capability ->
                    PermissionItem(
                        capability = capability,
                        rationale = plugin.getPermissionRationale()[capability]
                    )
                }
                
                // Privacy policy
                val privacyPolicyText = plugin.securityManifest.privacyPolicy
                if (!privacyPolicyText.isNullOrBlank()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = "Privacy Policy",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = privacyPolicyText,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = onGrant,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary
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
fun PermissionItem(
    capability: PluginCapability,
    rationale: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (capability.getRiskLevel()) {
                RiskLevel.LOW -> MaterialTheme.colorScheme.surface
                RiskLevel.MEDIUM -> MaterialTheme.colorScheme.secondaryContainer
                RiskLevel.HIGH -> MaterialTheme.colorScheme.tertiaryContainer
                RiskLevel.CRITICAL -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Risk indicator
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        when (capability.getRiskLevel()) {
                            RiskLevel.LOW -> MaterialTheme.colorScheme.primary
                            RiskLevel.MEDIUM -> MaterialTheme.colorScheme.secondary
                            RiskLevel.HIGH -> MaterialTheme.colorScheme.tertiary
                            RiskLevel.CRITICAL -> MaterialTheme.colorScheme.error
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getIconForCapability(capability),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.size(16.dp)
                )
            }
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = capability.name.replace("_", " ").lowercase().capitalize(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = rationale ?: capability.getDescription(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Risk badge
            Text(
                text = capability.getRiskLevel().name,
                style = MaterialTheme.typography.labelSmall,
                color = when (capability.getRiskLevel()) {
                    RiskLevel.LOW -> MaterialTheme.colorScheme.primary
                    RiskLevel.MEDIUM -> MaterialTheme.colorScheme.secondary
                    RiskLevel.HIGH -> MaterialTheme.colorScheme.tertiary
                    RiskLevel.CRITICAL -> MaterialTheme.colorScheme.error
                }
            )
        }
    }
}

@Composable
fun PluginTrustLevelCard(trustLevel: PluginTrustLevel) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = when (trustLevel) {
                PluginTrustLevel.OFFICIAL -> MaterialTheme.colorScheme.primaryContainer
                PluginTrustLevel.VERIFIED -> MaterialTheme.colorScheme.secondaryContainer
                PluginTrustLevel.COMMUNITY -> MaterialTheme.colorScheme.surfaceVariant
                PluginTrustLevel.UNTRUSTED -> MaterialTheme.colorScheme.errorContainer
                PluginTrustLevel.BLOCKED -> MaterialTheme.colorScheme.error
                PluginTrustLevel.QUARANTINED -> MaterialTheme.colorScheme.errorContainer
            }
        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimplePermissionDialog(
    capability: PluginCapability,
    onGrant: () -> Unit,
    onDeny: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDeny,
        title = {
            Text("Grant Permission?")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Grant ${capability.name.replace("_", " ").lowercase()} permission?",
                    style = MaterialTheme.typography.bodyLarge
                )
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = capability.getDescription(),
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                if (capability.getRiskLevel() != RiskLevel.LOW) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = AppIcons.Status.warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Risk Level: ${capability.getRiskLevel()}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
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
