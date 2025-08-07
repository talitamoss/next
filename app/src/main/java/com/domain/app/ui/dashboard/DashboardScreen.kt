package com.domain.app.ui.dashboard

import com.domain.app.core.data.DataPoint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.domain.app.core.plugin.Plugin
import com.domain.app.ui.theme.AppIcons
import com.domain.app.ui.utils.getPluginIcon
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToPlugin: (Plugin) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    var showQuickAddDialog by remember { mutableStateOf(false) }
    var showPluginSelector by remember { mutableStateOf(false) }
    var selectedPlugin by remember { mutableStateOf<Plugin?>(null) }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Behavioral Data") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = AppIcons.Navigation.settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showPluginSelector = true }
            ) {
                Icon(
                    imageVector = AppIcons.Action.add,
                    contentDescription = "Quick Add"
                )
            }
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Summary cards spanning full width
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "Today's Summary",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            
            item {
                SummaryCard(
                    title = "Data Points",
                    value = uiState.todayDataCount.toString()
                )
            }
            
            item {
                SummaryCard(
                    title = "Active Plugins",
                    value = uiState.activePlugins.size.toString()
                )
            }
            
            item {
                SummaryCard(
                    title = "Streak",
                    value = "${uiState.currentStreak} days"
                )
            }
            
            // Plugins section
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Your Plugins",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { /* Navigate to all plugins */ }) {
                        Text("See All")
                    }
                }
            }
            
            // Plugin tiles
            items(uiState.availablePlugins) { plugin ->
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
                    onClick = { /* Navigate to plugin store */ }
                )
            }
            
            // Empty tiles for layout balance
            val remainingTiles = (4 - (uiState.availablePlugins.size + 1) % 4) % 4
            repeat(remainingTiles) {
                item {
                    EmptyPluginTile()
                }
            }
        }
    }
    
    // Quick add dialog
    if (showQuickAddDialog && selectedPlugin != null) {
        MultiStageQuickAddDialog(
            plugin = selectedPlugin!!,
            onDismiss = {
                showQuickAddDialog = false
                selectedPlugin = null
            },
            onConfirm = { dataPoint ->
                viewModel.quickAddData(selectedPlugin!!, dataPoint)
                showQuickAddDialog = false
                selectedPlugin = null
                scope.launch {
                    snackbarHostState.showSnackbar("Data added successfully")
                }
            }
        )
    }
    
    // Plugin selector bottom sheet
    if (showPluginSelector) {
        PluginSelectorBottomSheet(
            plugins = uiState.availablePlugins,
            onPluginSelected = { plugin ->
                selectedPlugin = plugin
                showPluginSelector = false
                showQuickAddDialog = true
            },
            onDismiss = {
                showPluginSelector = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginTile(
    plugin: Plugin,
    dataCount: Int,
    onClick: () -> Unit,
    onQuickAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Background gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        )
                    )
            )
            
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
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getPluginIcon(plugin),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    if (dataCount > 0) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = dataCount.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                
                // Plugin info
                Column {
                    Text(
                        text = plugin.metadata.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = plugin.metadata.description.take(50) + if (plugin.metadata.description.length > 50) "..." else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Quick add button
                Button(
                    onClick = onQuickAdd,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 4.dp)
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
}

@Composable
fun SummaryCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun AddPluginTile(
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                    contentDescription = "Add Plugin",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
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
fun EmptyPluginTile() {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    )
}

@Composable
fun RecentActivityCard(
    activities: List<RecentActivity>,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Activity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onViewAll) {
                    Text("View All")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (activities.isEmpty()) {
                Text(
                    text = "No recent activity",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                activities.take(5).forEach { activity ->
                    ActivityItem(activity)
                    if (activity != activities.last()) {
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityItem(activity: RecentActivity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = getPluginIcon(activity.plugin),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = activity.title,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = activity.timeAgo,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Text(
            text = activity.value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

// Data class for recent activity
data class RecentActivity(
    val plugin: Plugin,
    val title: String,
    val value: String,
    val timeAgo: String
)
