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
import com.domain.app.core.plugin.PluginState
import com.domain.app.ui.components.plugin.quickadd.UnifiedQuickAddDialog  // MIGRATED: New import
import com.domain.app.ui.theme.AppIcons
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Main dashboard screen for the app.
 * MIGRATED: Now using UnifiedQuickAddDialog for all plugin quick-add operations.
 * This reduces code complexity and ensures consistent UI across all plugin types.
 * 
 * Changes from previous version:
 * - Removed individual dialog imports (WaterQuickAddDialog, MoodQuickAddDialog, etc.)
 * - Replaced complex when/if logic with single UnifiedQuickAddDialog
 * - Reduced dialog code from ~50 lines to ~10 lines
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
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { /* Handle navigation */ }) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(
                            imageVector = AppIcons.Navigation.settings,
                            contentDescription = "Settings"
                        )
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
                horizontalArrangement = Arrangement.spacedBy(12.dp)
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

    // ==================== MIGRATED DIALOG SECTION ====================
    // This section has been significantly simplified by using UnifiedQuickAddDialog
    // Previous: 50+ lines with multiple when/if branches for different plugin types
    // Current: 10 lines that handle ALL plugin types automatically
    
    // Quick Add Dialog - Using UnifiedQuickAddDialog for ALL plugins
    val currentPlugin = selectedPlugin
    if (uiState.showQuickAdd && currentPlugin != null) {
        if (uiState.needsPermission) {
            // Permission dialog remains the same
            PluginPermissionQuickDialog(
                plugin = currentPlugin,
                onGrant = { viewModel.grantQuickAddPermission() },
                onDeny = { viewModel.dismissQuickAdd() }
            )
        } else {
            // MIGRATED: Single dialog handles all plugin types!
            // The UnifiedQuickAddDialog automatically detects:
            // - Multi-stage plugins (exercise, etc.)
            // - Slider plugins (water intake)
            // - Scale plugins (mood rating)
            // - Choice plugins (activity type)
            // - Generic text input (fallback)
            UnifiedQuickAddDialog(
                plugin = currentPlugin,
                onDismiss = { viewModel.dismissQuickAdd() },
                onConfirm = { data ->
                    viewModel.onQuickAdd(currentPlugin, data)
                }
            )
        }
    }
    // ==================== END OF MIGRATED SECTION ====================
    
    // Plugin Selector Bottom Sheet (unchanged)
    if (uiState.showPluginSelector) {
        PluginSelectorBottomSheet(
            availablePlugins = uiState.allPlugins.filter { plugin ->
                !uiState.dashboardPlugins.any { it.id == plugin.id }
            },
            onDismiss = { viewModel.dismissPluginSelector() },
            onSelect = { plugin ->
                viewModel.addPluginToDashboard(plugin)
                viewModel.dismissPluginSelector()
            }
        )
    }
    
    // Success feedback (unchanged)
    if (uiState.showSuccessFeedback) {
        LaunchedEffect(Unit) {
            delay(2000)
            viewModel.clearSuccessFeedback()
        }
    }
    
    // Error handling
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            scope.launch {
                // Show error snackbar or toast
                delay(3000)
                viewModel.clearError()
            }
        }
    }
}

/**
 * Plugin tile component for the dashboard grid
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardPluginTile(
    plugin: Plugin,
    isCollecting: Boolean,
    hasPermissions: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current
    
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (hasPermissions) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Plugin icon
                Text(
                    text = plugin.metadata.icon,
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                
                // Plugin name and status
                Column {
                    Text(
                        text = plugin.metadata.name,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Medium
                    )
                    
                    if (!hasPermissions) {
                        Text(
                            text = "Tap to enable",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Loading indicator
            if (isCollecting) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(16.dp)
                        .align(Alignment.TopEnd),
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

/**
 * Add plugin tile
 */
@Composable
fun AddPluginTile(
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .combinedClickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Plugin",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Add Plugin",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Empty plugin tile placeholder
 */
@Composable
fun EmptyPluginTile() {
    Spacer(
        modifier = Modifier.aspectRatio(1f)
    )
}

/**
 * Summary card component
 */
@Composable
fun SummaryCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
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
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Plugin permission dialog
 */
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
            Text("Enable ${plugin.metadata.name}?")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("This plugin needs the following permissions:")
                
                plugin.securityManifest.requestedCapabilities.forEach { capability ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = when (capability) {
                                PluginCapability.COLLECT_DATA -> Icons.Default.Edit
                                PluginCapability.READ_OWN_DATA -> Icons.Default.Visibility
                                PluginCapability.LOCAL_STORAGE -> Icons.Default.Storage
                                else -> Icons.Default.Security
                            },
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = capability.name.replace('_', ' ').lowercase()
                                .replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyMedium
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
                Text("Cancel")
            }
        }
    )
}

/**
 * Plugin selector bottom sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginSelectorBottomSheet(
    availablePlugins: List<Plugin>,
    onDismiss: () -> Unit,
    onSelect: (Plugin) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Add Plugin to Dashboard",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )
            
            if (availablePlugins.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "All plugins are already on your dashboard",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                availablePlugins.forEach { plugin ->
                    ListItem(
                        headlineContent = { 
                            Text(plugin.metadata.name)
                        },
                        supportingContent = { 
                            Text(plugin.metadata.description)
                        },
                        leadingContent = {
                            Text(
                                text = plugin.metadata.icon,
                                style = MaterialTheme.typography.headlineMedium
                            )
                        },
                        modifier = Modifier.clickable {
                            onSelect(plugin)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Extension function to add plugin to dashboard
 */
fun DashboardViewModel.addPluginToDashboard(plugin: Plugin) {
    // Implementation handled by ViewModel
}
