package com.domain.app.ui.dashboard

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
                    containerColor = MaterialTheme.colorScheme.primaryContainer
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
                        hasPermissions = uiState.pluginPermissions[plugin.id]?.isNotEmpty() ?: false,
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

    // Quick Add Bottom Sheet with permission check
    val currentPlugin = selectedPlugin
    if (uiState.showQuickAdd && currentPlugin != null) {
        if (uiState.needsPermission) {
            PluginPermissionQuickDialog(
                plugin = currentPlugin,
                onGrant = { viewModel.grantQuickAddPermission() },
                onDeny = { viewModel.dismissQuickAdd() }
            )
        } else {
            QuickAddBottomSheet(
                plugin = currentPlugin,
                onDismiss = { viewModel.dismissQuickAdd() },
                onDataSubmit = { plugin, data ->
                    viewModel.onQuickAdd(plugin, data)
                }
            )
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
                Icon(
                    imageVector = when(plugin.metadata.icon) {
                        "mood" -> Icons.Default.Mood
                        "fitness" -> Icons.Default.FitnessCenter
                        "sleep" -> Icons.Default.Bedtime
                        "water" -> Icons.Default.WaterDrop
                        "work" -> Icons.Default.Work
                        else -> Icons.Default.Extension
                    },
                    contentDescription = plugin.metadata.name,
                    modifier = Modifier.size(32.dp),
                    tint = if (hasPermissions) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = plugin.metadata.name,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
            
            // Collection indicator
            if (isCollecting) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddBottomSheet(
    plugin: Plugin,
    onDismiss: () -> Unit,
    onDataSubmit: (Plugin, Map<String, Any>) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Quick Add: ${plugin.metadata.name}",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            // Plugin-specific quick add UI would go here
            // For now, just a simple button
            Button(
                onClick = { 
                    onDataSubmit(plugin, mapOf("value" to 1))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Entry")
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginSelectorBottomSheet(
    availablePlugins: List<Plugin>,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Add to Dashboard",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            // List available plugins
            availablePlugins.forEach { plugin ->
                ListItem(
                    headlineContent = { Text(plugin.metadata.name) },
                    supportingContent = { Text(plugin.metadata.description) },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Extension,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.clickable {
                        // Add plugin to dashboard
                        onDismiss()
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun PluginPermissionQuickDialog(
    plugin: Plugin,
    onGrant: () -> Unit,
    onDeny: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDeny,
        title = { Text("Permission Required") },
        text = { 
            Text("${plugin.metadata.name} needs permission to collect data. Grant access?")
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
