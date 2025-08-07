package com.domain.app.ui.settings

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
import com.domain.app.core.plugin.Plugin
import com.domain.app.core.plugin.PluginCapability
import com.domain.app.core.plugin.security.PluginPermissionDialog
import com.domain.app.ui.theme.AppIcons
import com.domain.app.ui.utils.getPluginIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlugin: (Plugin) -> Unit,
    viewModel: PluginsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showPermissionDialog by remember { mutableStateOf(false) }
    var selectedPlugin by remember { mutableStateOf<Plugin?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Plugins") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = AppIcons.Navigation.back,
                            contentDescription = "Back"
                        )
                    }
                }
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
            items(uiState.plugins) { plugin ->
                PluginCard(
                    plugin = plugin,
                    isEnabled = uiState.enabledPlugins.contains(plugin.id),
                    onToggle = { enabled ->
                        if (enabled) {
                            selectedPlugin = plugin
                            showPermissionDialog = true
                        } else {
                            viewModel.disablePlugin(plugin)
                        }
                    },
                    onClick = { onNavigateToPlugin(plugin) }
                )
            }
        }
    }
    
    // Permission dialog
    if (showPermissionDialog && selectedPlugin != null) {
        PluginPermissionDialog(
            plugin = selectedPlugin!!,
            requestedPermissions = selectedPlugin!!.securityManifest.requestedCapabilities,
            onGrant = {
                viewModel.enablePlugin(selectedPlugin!!)
                showPermissionDialog = false
                selectedPlugin = null
            },
            onDeny = {
                showPermissionDialog = false
                selectedPlugin = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PluginCard(
    plugin: Plugin,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getPluginIcon(plugin),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (isEnabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = plugin.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = plugin.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle
            )
        }
    }
}
