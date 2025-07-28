package com.domain.app.ui.security

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.domain.app.core.plugin.PluginCapability
import com.domain.app.core.plugin.getDescription
import com.domain.app.core.plugin.getRiskLevel
import com.domain.app.core.plugin.security.*
import com.domain.app.ui.theme.AppIcons
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityHistoryDialog(
    events: List<SecurityEvent>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.8f),
        title = {
            Text("Security History")
        },
        text = {
            if (events.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No security events recorded",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(events.sortedByDescending { getEventTimestamp(it) }) { event ->
                        SecurityEventCard(event)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun SecurityEventCard(event: SecurityEvent) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = when (event) {
                is SecurityEvent.SecurityViolation -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                is SecurityEvent.PermissionDenied -> MaterialTheme.colorScheme.tertiaryContainer
                is SecurityEvent.PermissionGranted -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = getEventIcon(event),
                contentDescription = null,
                tint = getEventColor(event),
                modifier = Modifier.size(24.dp)
            )
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = getEventTitle(event),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = getEventDescription(event),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatTimestamp(getEventTimestamp(event)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
                
                if (capability.getRiskLevel() != com.domain.app.core.plugin.RiskLevel.LOW) {
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

private fun getEventIcon(event: SecurityEvent): androidx.compose.ui.graphics.vector.ImageVector {
    return when (event) {
        is SecurityEvent.PermissionRequested -> AppIcons.Status.info
        is SecurityEvent.PermissionGranted -> AppIcons.Status.success
        is SecurityEvent.PermissionDenied -> AppIcons.Action.delete
        is SecurityEvent.SecurityViolation -> AppIcons.Status.error
        is SecurityEvent.DataAccess -> AppIcons.Storage.folder
    }
}

@Composable
private fun getEventColor(event: SecurityEvent): androidx.compose.ui.graphics.Color {
    return when (event) {
        is SecurityEvent.PermissionGranted -> MaterialTheme.colorScheme.primary
        is SecurityEvent.SecurityViolation -> MaterialTheme.colorScheme.error
        is SecurityEvent.PermissionDenied -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

private fun getEventTitle(event: SecurityEvent): String {
    return when (event) {
        is SecurityEvent.PermissionRequested -> "Permission Requested"
        is SecurityEvent.PermissionGranted -> "Permission Granted"
        is SecurityEvent.PermissionDenied -> "Permission Denied"
        is SecurityEvent.SecurityViolation -> "Security Violation"
        is SecurityEvent.DataAccess -> "Data Access"
    }
}

private fun getEventDescription(event: SecurityEvent): String {
    return when (event) {
        is SecurityEvent.PermissionRequested -> "Requested ${event.capability.name.lowercase()}"
        is SecurityEvent.PermissionGranted -> "Granted ${event.capability.name.lowercase()} by ${event.grantedBy}"
        is SecurityEvent.PermissionDenied -> event.reason
        is SecurityEvent.SecurityViolation -> "${event.violationType}: ${event.details}"
        is SecurityEvent.DataAccess -> "${event.accessType} ${event.recordCount} ${event.dataType} records"
    }
}

private fun getEventTimestamp(event: SecurityEvent): Long {
    return when (event) {
        is SecurityEvent.PermissionRequested -> event.timestamp
        is SecurityEvent.PermissionGranted -> event.timestamp
        is SecurityEvent.PermissionDenied -> event.timestamp
        is SecurityEvent.SecurityViolation -> event.timestamp
        is SecurityEvent.DataAccess -> event.timestamp
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}
