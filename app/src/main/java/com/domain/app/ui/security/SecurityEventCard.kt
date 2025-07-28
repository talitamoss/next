package com.domain.app.ui.security

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.domain.app.core.plugin.security.*
import com.domain.app.ui.theme.AppIcons
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SecurityEventCard(
    event: SecurityEvent,
    pluginName: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = pluginName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = formatTimestamp(getEventTimestamp(event)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = getEventDescription(event),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
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

private fun getEventColor(event: SecurityEvent): androidx.compose.ui.graphics.Color {
    return when (event) {
        is SecurityEvent.PermissionGranted -> androidx.compose.material3.MaterialTheme.colorScheme.primary
        is SecurityEvent.SecurityViolation -> androidx.compose.material3.MaterialTheme.colorScheme.error
        is SecurityEvent.PermissionDenied -> androidx.compose.material3.MaterialTheme.colorScheme.tertiary
        else -> androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
    }
}

private fun getEventDescription(event: SecurityEvent): String {
    return when (event) {
        is SecurityEvent.PermissionRequested -> "Requested ${event.capability.name.lowercase().replace("_", " ")}"
        is SecurityEvent.PermissionGranted -> "Granted ${event.capability.name.lowercase().replace("_", " ")} by ${event.grantedBy}"
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
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60000 -> "Just now"
        diff < 3600000 -> "${diff / 60000}m ago"
        diff < 86400000 -> "${diff / 3600000}h ago"
        else -> {
            val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
            dateFormat.format(Date(timestamp))
        }
    }
}
