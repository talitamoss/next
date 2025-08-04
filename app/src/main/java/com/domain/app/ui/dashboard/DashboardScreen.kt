package com.domain.app.ui.dashboard

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import com.domain.app.ui.theme.AppIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.domain.app.core.plugin.Plugin
import com.domain.app.core.plugin.security.PluginTrustLevel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedPlugin by viewModel.selectedPlugin.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    TextButton(
                        onClick = { 
                            navController.navigate("settings") {
                                popUpTo(navController.graph.id) {
                                    inclusive = false
                                }
                            }
                        }
                    ) {
                        Text("Manage")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Summary Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryCard(
                    title = "Today",
                    value = "${uiState.todayEntryCount}",
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "This Week",
                    value = "${uiState.weekEntryCount}",
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "Active",
                    value = "${uiState.activePluginCount}",
                    modifier = Modifier.weight(1f)
                )
            }

            // Dashboard Grid - Fixed 2x3 grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Show dashboard plugins
                items(uiState.dashboardPlugins) { plugin ->
                    DashboardPluginTile(
                        plugin = plugin,
                        isCollecting = uiState.pluginStates[plugin.id]?.isCollecting ?: false,
                        hasPermissions = uiState.pluginPermissions[plugin.id] ?: false,
                        onClick = {
                            viewModel.onPluginTileClick(plugin)
                        },
                        onLongClick = {
                            navController.navigate("plugin_security/${plugin.id}")
                        }
                    )
                }
                
                // Add empty slots if less than 6 plugins
                val emptySlots = 6 - uiState.dashboardPlugins.size
                if (emptySlots > 0 && uiState.canAddMorePlugins) {
                    item {
                        AddPluginTile(
                            onClick = { viewModel.onAddPluginClick() }
                        )
                    }
                    
                    // Fill remaining slots with empty tiles
                    repeat(emptySlots - 1) {
                        item {
                            EmptyPluginTile()
                        }
                    }
                }
            }
        }
    }

    // Direct plugin dialogs - no intermediate bottom sheet
    val currentPlugin = selectedPlugin
    if (uiState.showQuickAdd && currentPlugin != null) {
        if (uiState.needsPermission) {
            PluginPermissionQuickDialog(
                plugin = currentPlugin,
                onGrant = { viewModel.grantQuickAddPermission() },
                onDeny = { viewModel.dismissQuickAdd() }
            )
        } else {
            // Show plugin-specific dialog directly
            when {
                // Multi-stage plugins
                currentPlugin.getQuickAddStages() != null -> {
                    MultiStageQuickAddDialog(
                        plugin = currentPlugin,
                        stages = currentPlugin.getQuickAddStages()!!,
                        onDismiss = { viewModel.dismissQuickAdd() },
                        onComplete = { stageData ->
                            viewModel.onQuickAdd(currentPlugin, stageData)
                        }
                    )
                }
                // Plugin-specific dialogs
                currentPlugin.id == "water" -> {
                    val config = currentPlugin.getQuickAddConfig()
                    if (config != null) {
                        WaterQuickAddDialog(
                            options = config.options ?: emptyList(),
                            onDismiss = { viewModel.dismissQuickAdd() },
                            onConfirm = { amount ->
                                viewModel.onQuickAdd(currentPlugin, mapOf("amount" to amount))
                            }
                        )
                    }
                }
                currentPlugin.id == "mood" -> {
                    val config = currentPlugin.getQuickAddConfig()
                    if (config != null) {
                        MoodQuickAddDialog(
                            options = config.options ?: emptyList(),
                            onDismiss = { viewModel.dismissQuickAdd() },
                            onConfirm = { moodValue, note ->
                                val data = mutableMapOf<String, Any>("value" to moodValue)
                                note?.let { data["note"] = it }
                                viewModel.onQuickAdd(currentPlugin, data)
                            }
                        )
                    }
                }
                // Generic dialog for other plugins
                else -> {
                    GenericQuickAddDialog(
                        plugin = currentPlugin,
                        onDismiss = { viewModel.dismissQuickAdd() },
                        onConfirm = { data ->
                            viewModel.onQuickAdd(currentPlugin, data)
                        }
                    )
                }
            }
        }
    }
    
    // Plugin Selector Bottom Sheet
    if (uiState.showPluginSelector) {
        PluginSelectorBottomSheet(
            availablePlugins = uiState.allPlugins.filter { plugin ->
                !uiState.dashboardPlugins.any { it.id == plugin.id }
            },
            onDismiss = { viewModel.dismissPluginSelector() }
        )
    }
    
    // Success feedback
    if (uiState.showSuccessFeedback) {
        LaunchedEffect(Unit) {
            delay(2000)
            viewModel.clearSuccessFeedback()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardPluginTile(
    plugin: Plugin,
    isCollecting: Boolean,
    hasPermissions: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = when {
                !hasPermissions -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                isCollecting -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Plugin icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (hasPermissions) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.getPluginIcon(plugin.id),
                        contentDescription = null,
                        tint = if (hasPermissions)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = plugin.metadata.name,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    color = if (hasPermissions)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!hasPermissions) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Needs permission",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (isCollecting) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Active",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // Trust indicator
            if (plugin.trustLevel == PluginTrustLevel.OFFICIAL) {
                Icon(
                    imageVector = AppIcons.Security.shield,
                    contentDescription = "Official plugin",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginPermissionQuickDialog(
    plugin: Plugin,
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
                Icon(
                    imageVector = AppIcons.getPluginIcon(plugin.id),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("Enable ${plugin.metadata.name}?")
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("This plugin needs permissions to collect data.")
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text(
                        text = "Required: ${plugin.securityManifest.requestedCapabilities.size} permissions",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                Text(
                    text = "You can review detailed permissions in Settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            FilledTonalButton(onClick = onGrant) {
                Text("Grant & Continue")
            }
        },
        dismissButton = {
            TextButton(onClick = onDeny) {
                Text("Cancel")
            }
        }
    )
}
