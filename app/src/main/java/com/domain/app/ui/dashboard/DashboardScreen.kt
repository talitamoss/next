package com.domain.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
                        onClick = { navController.navigate("settings") }
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
                        onClick = {
                            viewModel.onPluginTileClick(plugin)
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

    // Quick Add Bottom Sheet
    if (uiState.showQuickAdd && selectedPlugin != null) {
        QuickAddBottomSheet(
            plugin = selectedPlugin,
            onDismiss = { viewModel.dismissQuickAdd() },
            onDataSubmit = { plugin, data ->
                viewModel.onQuickAdd(plugin, data)
            }
        )
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

@Composable
fun SummaryCard(
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
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun DashboardPluginTile(
    plugin: Plugin,
    isCollecting: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isCollecting) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = AppIcons.getPluginIcon(plugin.id),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = plugin.metadata.name,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            if (isCollecting) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Active",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun AddPluginTile(
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = AppIcons.Action.add,
                contentDescription = "Add plugin",
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Add Plugin",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EmptyPluginTile() {
    Card(
        modifier = Modifier.aspectRatio(1f),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        )
    ) {
        Box(modifier = Modifier.fillMaxSize())
    }
}
