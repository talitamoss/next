package com.domain.app.core.plugin.security

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.domain.app.core.plugin.PluginCapability

/**
 * Plugin security management screen
 */
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
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.plugin == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Plugin Info Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Extension,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = uiState.plugin?.name ?: "",
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Text(
                                text = "Trust Level: ${uiState.trustLevel}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                
                // Permissions Section
                item {
                    Text(
                        text = "Permissions",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                
                items(PluginCapability.values()) { capability ->
                    val isGranted = uiState.grantedPermissions.contains(capability)
                    
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = capability.displayName,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = capability.description,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Switch(
                                checked = isGranted,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        viewModel.grantPermission(capability)
                                    } else {
                                        viewModel.denyPermission(capability)
                                    }
                                }
                            )
                        }
                    }
                }
                
                // Security Log Section
                if (uiState.securityLogs.isNotEmpty()) {
                    item {
                        Text(
                            text = "Security Log",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    
                    items(uiState.securityLogs) { log ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            ListItem(
                                headlineContent = { Text(log.event) },
                                supportingContent = { 
                                    Text(log.timestamp.toString())
                                },
                                leadingContent = {
                                    Icon(
                                        imageVector = when(log.event) {
                                            "Permission Granted" -> Icons.Default.CheckCircle
                                            "Permission Revoked" -> Icons.Default.Cancel
                                            else -> Icons.Default.Info
                                        },
                                        contentDescription = null
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Permission Request Dialog - removed since the existing ViewModel handles permissions directly
    // The existing ViewModel uses grantPermission/denyPermission without a dialog flow
}

// Extension properties for display
private val PluginCapability.displayName: String
    get() = when(this) {
        PluginCapability.COLLECT_DATA -> "Collect Data"
        PluginCapability.SHARE_DATA -> "Share Data"
        PluginCapability.NETWORK_ACCESS -> "Network Access"
        PluginCapability.BACKGROUND_PROCESSING -> "Background Processing"
        PluginCapability.NOTIFICATION -> "Send Notifications"
        PluginCapability.STORAGE_ACCESS -> "Storage Access"
        PluginCapability.SENSOR_ACCESS -> "Sensor Access"
    }

private val PluginCapability.description: String
    get() = when(this) {
        PluginCapability.COLLECT_DATA -> "Allow plugin to collect and store data"
        PluginCapability.SHARE_DATA -> "Allow plugin to share data with other plugins"
        PluginCapability.NETWORK_ACCESS -> "Allow plugin to access the internet"
        PluginCapability.BACKGROUND_PROCESSING -> "Allow plugin to run in background"
        PluginCapability.NOTIFICATION -> "Allow plugin to send notifications"
        PluginCapability.STORAGE_ACCESS -> "Allow plugin to access device storage"
        PluginCapability.SENSOR_ACCESS -> "Allow plugin to access device sensors"
    }
