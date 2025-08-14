package com.domain.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.domain.app.core.plugin.Plugin
import com.domain.app.ui.theme.AppIcons
import com.domain.app.ui.utils.getPluginIcon
import com.domain.app.ui.components.plugin.quickadd.UnifiedQuickAddDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToPlugin: (Plugin) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showPluginSelector by remember { mutableStateOf(false) }
    var showQuickAddDialog by remember { mutableStateOf(false) }
    var selectedPlugin by remember { mutableStateOf<Plugin?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Dashboard",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = AppIcons.Navigation.settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header section
            item(span = { GridItemSpan(2) }) {
                DashboardHeader(
                    totalPlugins = uiState.dashboardPlugins.size,
                    // FIX 1: Calculate activeToday from pluginDataCounts
                    activeToday = uiState.pluginDataCounts.count { it.value > 0 }
                )
            }
            
            // Plugin tiles
            items(uiState.dashboardPlugins) { plugin ->
                PluginTile(
                    plugin = plugin,
                    dataCount = uiState.pluginDataCounts[plugin.id] ?: 0,
                    onClick = { onNavigateToPlugin(plugin) },
                    onQuickAdd = {
                        selectedPlugin = plugin
                        showQuickAddDialog = true
                    }
                )
            }
            
            // Add plugin tile
            item {
                AddPluginTile(
                    onClick = { showPluginSelector = true }
                )
            }
            
            // Empty tiles for layout balance
            val pluginCount = uiState.dashboardPlugins.size
            val remainingTiles = (4 - (pluginCount + 1) % 4) % 4
            repeat(remainingTiles) {
                item {
                    EmptyPluginTile()
                }
            }
        }
    }
    
    // Quick add dialog - Using UnifiedQuickAddDialog
    if (showQuickAddDialog && selectedPlugin != null) {
        UnifiedQuickAddDialog(
            plugin = selectedPlugin!!,
            onDismiss = {
                showQuickAddDialog = false
                selectedPlugin = null
            },
            onConfirm = { data ->
                viewModel.onQuickAdd(selectedPlugin!!, data)
                showQuickAddDialog = false
                selectedPlugin = null
            }
        )
    }
    
    // Plugin selector bottom sheet
    if (showPluginSelector) {
        PluginSelectorBottomSheet(
            plugins = uiState.allPlugins,
            // FIX 2: Explicit type annotation for lambda parameter
            onPluginSelected = { plugin: Plugin ->
                viewModel.addPluginToDashboard(plugin.id)
                showPluginSelector = false
            },
            onDismiss = {
                showPluginSelector = false
            }
        )
    }
}

@Composable
fun DashboardHeader(
    totalPlugins: Int,
    activeToday: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Welcome back!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "Active Plugins",
                    value = totalPlugins.toString()
                )
                StatItem(
                    label = "Tracked Today",
                    value = activeToday.toString()
                )
            }
        }
    }
}

@Composable
fun StatItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PluginTile(
    plugin: Plugin,
    dataCount: Int,
    onClick: () -> Unit,
    onQuickAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Icon and count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = getPluginIcon(plugin),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                if (dataCount > 0) {
                    Badge {
                        Text(
                            text = dataCount.toString(),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
            
            // Plugin name
            Text(
                text = plugin.metadata.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            
            // Quick add button
            Button(
                onClick = onQuickAdd,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = AppIcons.Action.add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Quick Add",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
fun AddPluginTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable { onClick() },
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = AppIcons.Action.add,
                    contentDescription = null,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .padding(4.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Add Plugin",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EmptyPluginTile(
    modifier: Modifier = Modifier
) {
    Spacer(
        modifier = modifier.aspectRatio(1f)
    )
}

// FIX 3: REMOVED duplicate PluginSelectorBottomSheet
// This function already exists in PluginSelectorBottomSheet.kt
