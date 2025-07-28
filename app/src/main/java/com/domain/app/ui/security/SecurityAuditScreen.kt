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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.domain.app.core.plugin.security.SecurityEvent
import com.domain.app.ui.theme.AppIcons
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityAuditScreen(
    navController: NavController,
    viewModel: SecurityAuditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Security Audit") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(AppIcons.Navigation.back, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshEvents() }) {
                        Icon(AppIcons.Action.refresh, contentDescription = "Refresh")
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Summary card
            item {
                SecuritySummaryCard(
                    totalEvents = uiState.totalEvents,
                    violations = uiState.violations,
                    highRiskPlugins = uiState.highRiskPlugins
                )
            }
            
            // Active violations
            if (uiState.activeViolations.isNotEmpty()) {
                item {
                    Text(
                        text = "Active Violations",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                items(uiState.activeViolations) { (pluginId, violations) ->
                    ViolationCard(
                        pluginName = uiState.pluginNames[pluginId] ?: pluginId,
                        violations = violations,
                        onViewPlugin = {
                            navController.navigate("plugin_security/$pluginId")
                        }
                    )
                }
            }
            
            // Recent events
            item {
                Text(
                    text = "Recent Security Events",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            items(uiState.recentEvents) { event ->
                SecurityEventCard(
                    event = event,
                    pluginName = when (event) {
                        is SecurityEvent.PermissionRequested -> uiState.pluginNames[event.pluginId]
                        is SecurityEvent.PermissionGranted -> uiState.pluginNames[event.pluginId]
                        is SecurityEvent.PermissionDenied -> uiState.pluginNames[event.pluginId]
                        is SecurityEvent.SecurityViolation -> uiState.pluginNames[event.pluginId]
                        is SecurityEvent.DataAccess -> uiState.pluginNames[event.pluginId]
                    } ?: "Unknown"
                )
            }
        }
    }
}

@Composable
fun SecuritySummaryCard(
    totalEvents: Int,
    violations: Int,
    highRiskPlugins: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Security Overview",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricColumn(
                    value = totalEvents.toString(),
                    label = "Total Events",
                    icon = AppIcons.Status.info
                )
                MetricColumn(
                    value = violations.toString(),
                    label = "Violations",
                    icon = AppIcons.Status.warning,
                    color = if (violations > 0) MaterialTheme.colorScheme.error else null
                )
                MetricColumn(
                    value = highRiskPlugins.toString(),
                    label = "High Risk",
                    icon = AppIcons.Status.error,
                    color = if (highRiskPlugins > 0) MaterialTheme.colorScheme.error else null
                )
            }
        }
    }
}

@Composable
fun MetricColumn(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color ?: MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color ?: MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun ViolationCard(
    pluginName: String,
    violations: List<SecurityEvent.SecurityViolation>,
    onViewPlugin: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = pluginName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                TextButton(onClick = onViewPlugin) {
                    Text("View")
                }
            }
            
            Text(
                text = "${violations.size} violations",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
            
            val latestViolation = violations.maxByOrNull { it.timestamp }
            latestViolation?.let {
                Text(
                    text = "Latest: ${it.violationType}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
