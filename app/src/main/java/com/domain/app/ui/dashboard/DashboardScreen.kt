// app/src/main/java/com/domain/app/ui/dashboard/DashboardScreen.kt
package com.domain.app.ui.dashboard

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.domain.app.core.plugin.Plugin
import com.domain.app.core.plugin.PluginCapability
import com.domain.app.ui.components.plugin.quickadd.UnifiedQuickAddDialog
import com.domain.app.ui.theme.AppIcons
import com.domain.app.ui.utils.getPluginIcon
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Main dashboard screen for the app
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedPlugin by viewModel.selectedPlugin.collectAsState()
    val scope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Dashboard",
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    IconButton(onClick = { navController.navigate("data") }) {
                        Icon(AppIcons.Data.analytics, contentDescription = "View All Data")
                    }
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(AppIcons.Navigation.settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate("data") },
                text = { Text("Add Data") },
                icon = { Icon(AppIcons.Action.add, contentDescription = null) }
            )
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Summary Cards Row
            item(span = { GridItemSpan(2) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryCard(
                        title = "Today",
                        value = uiState.todayEntryCount.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        title = "This Week",
                        value = uiState.weekEntryCount.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        title = "Active",
                        value = uiState.activePluginCount.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // Plugin tiles
            items(
                items = uiState.dashboardPlugins,
                key = { it.id }
            ) { plugin ->
                PluginTile(
                    plugin = plugin,
                    isEnabled = uiState.pluginStates[plugin.id]?.isCollecting ?: false,
                    hasPermission = uiState.pluginPermissions[plugin.id] ?: false,
                    onClick = { 
                        viewModel.onPluginTileClick(plugin)
                    },
                    onLongClick = {
                        navController.navigate("plugin_detail/${plugin.id}")
                    }
                )
            }
            
            // Add plugin tile (if not at max)
            if (uiState.dashboardPlugins.size < 8 && uiState.canAddMorePlugins) {
                item {
                    AddPluginTile(
                        onClick = { viewModel.onAddPluginClick() }
                    )
                }
            }
            
            // Empty tiles to maintain grid
            val emptyTiles = (8 - uiState.dashboardPlugins.size - 1).coerceAtLeast(0)
            items(emptyTiles) {
                EmptyPluginTile()
            }
        }
    }
    
    // Quick Add Dialog
    selectedPlugin?.let { plugin ->
        if (uiState.showQuickAdd) {
            if (uiState.needsPermission) {
                PluginPermissionDialog(
                    plugin = plugin,
                    onGrant = { viewModel.grantQuickAddPermission() },
                    onDeny = { viewModel.dismissQuickAdd() }
                )
            } else {
                UnifiedQuickAddDialog(
                    plugin = plugin,
                    onDismiss = { viewModel.dismissQuickAdd() },
                    onSave = { data ->
                        viewModel.onQuickAdd(plugin.id, data)
                    }
                )
            }
        }
    }
    
    // Plugin selector bottom sheet
    if (uiState.showPluginSelector) {
        PluginSelectorBottomSheet(
            availablePlugins = uiState.allPlugins.filter { plugin ->
                !uiState.dashboardPlugins.any { it.id == plugin.id }
            },
            onDismiss = { viewModel.dismissPluginSelector() },
            onPluginSelected = { plugin ->
                viewModel.addPluginToDashboard(plugin.id)
                viewModel.dismissPluginSelector()
            }
        )
    }
    
    // Success feedback
    if (uiState.showSuccessFeedback) {
        LaunchedEffect(Unit) {
            delay(2000)
            viewModel.clearSuccessFeedback()
        }
    }
    
    // Error handling
    uiState.error?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Error") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }
}

/**
 * Plugin tile component
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PluginTile(
    plugin: Plugin,
    isEnabled: Boolean,
    hasPermission: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = getPluginIcon(plugin),
                contentDescription = plugin.metadata.name,
                modifier = Modifier.size(32.dp),
                tint = if (isEnabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            
            Column {
                Text(
                    text = plugin.metadata.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (!hasPermission) {
                    Text(
                        text = "Tap to enable",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Summary card component
 */
@Composable
private fun SummaryCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Add plugin tile
 */
@Composable
private fun AddPluginTile(
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Plugin",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Add Plugin",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Empty plugin tile (placeholder)
 */
@Composable
private fun EmptyPluginTile() {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    )
}

/**
 * Plugin permission dialog
 */
@Composable
private fun PluginPermissionDialog(
    plugin: Plugin,
    onGrant: () -> Unit,
    onDeny: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDeny,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = getPluginIcon(plugin),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(plugin.metadata.name)
            }
        },
        text = {
            Column {
                Text("This plugin requires the following permissions:")
                Spacer(modifier = Modifier.height(8.dp))
                plugin.securityManifest.requestedCapabilities.forEach { capability ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = capability.name.replace('_', ' ').lowercase()
                                .replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodySmall
                        )
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

/**
 * Plugin selector bottom sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PluginSelectorBottomSheet(
    availablePlugins: List<Plugin>,
    onDismiss: () -> Unit,
    onPluginSelected: (Plugin) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Add Plugin to Dashboard",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            availablePlugins.forEach { plugin ->
                ListItem(
                    headlineContent = { Text(plugin.metadata.name) },
                    leadingContent = {
                        Icon(
                            imageVector = getPluginIcon(plugin),
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.clickable {
                        onPluginSelected(plugin)
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
