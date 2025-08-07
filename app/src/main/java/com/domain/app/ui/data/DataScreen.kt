// app/src/main/java/com/domain/app/ui/data/DataScreen.kt
package com.domain.app.ui.data

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.domain.app.ui.components.core.feedback.LoadingOverlay
import com.domain.app.ui.components.core.feedback.EmptyState
import com.domain.app.ui.components.core.lists.SwipeableListItem
import com.domain.app.ui.theme.AppIcons
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Data management screen showing all collected data
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataScreen(
    navController: NavController,
    viewModel: DataViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // Handle messages and errors
    LaunchedEffect(uiState) {
        uiState.message?.let { message ->
            if (message != uiState.error) {
                scope.launch {
                    snackbarHostState.showSnackbar(message)
                }
                viewModel.clearMessage()
            }
        }
        uiState.error?.let { error ->
            scope.launch {
                snackbarHostState.showSnackbar(error)
            }
            viewModel.clearError()
        }
    }
    
    Scaffold(
        topBar = {
            if (uiState.isInSelectionMode) {
                SelectionModeTopBar(
                    selectedCount = uiState.selectedDataPoints.size,
                    onClearSelection = { viewModel.clearSelection() },
                    onSelectAll = { viewModel.selectAllDataPoints() },
                    onDelete = {
                        viewModel.deleteMultipleDataPoints(uiState.selectedDataPoints)
                    }
                )
            } else {
                TopAppBar(
                    title = { Text("Your Data") },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        // Filter menu
                        var showFilterMenu by remember { mutableStateOf(false) }
                        IconButton(onClick = { showFilterMenu = true }) {
                            Icon(AppIcons.Action.filter, contentDescription = "Filter")
                        }
                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Plugins") },
                                onClick = {
                                    viewModel.filterByPlugin(null)
                                    showFilterMenu = false
                                }
                            )
                            Divider()
                            uiState.pluginSummaries.forEach { (pluginId, summary) ->
                                DropdownMenuItem(
                                    text = { 
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(uiState.pluginNames[pluginId] ?: pluginId)
                                            Text(
                                                text = summary.count.toString(),
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.filterByPlugin(pluginId)
                                        showFilterMenu = false
                                    },
                                    leadingIcon = {
                                        if (uiState.selectedPluginFilter == pluginId) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                )
                            }
                        }
                        
                        // Export action
                        IconButton(onClick = { navController.navigate("export") }) {
                            Icon(AppIcons.Data.upload, contentDescription = "Export")
                        }
                        
                        // Refresh action
                        IconButton(onClick = { viewModel.refreshData() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!uiState.isInSelectionMode) {
                ExtendedFloatingActionButton(
                    onClick = { navController.navigate("quick_add") },
                    text = { Text("Add Data") },
                    icon = { Icon(AppIcons.Action.add, contentDescription = null) }
                )
            }
        }
    ) { paddingValues ->
        LoadingOverlay(
            isLoading = uiState.isLoading,
            message = "Loading data..."
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when {
                    uiState.dataPoints.isEmpty() -> {
                        EmptyState(
                            title = if (uiState.selectedPluginFilter != null) {
                                "No data for ${uiState.pluginNames[uiState.selectedPluginFilter]}"
                            } else {
                                "No data yet"
                            },
                            subtitle = "Start tracking to see your data here",
                            icon = AppIcons.Data.analytics,
                            actionLabel = "Add First Entry",
                            onAction = { navController.navigate("quick_add") }
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            // Summary header if filtered
                            if (uiState.selectedPluginFilter != null) {
                                item {
                                    DataSummaryCard(
                                        title = uiState.pluginNames[uiState.selectedPluginFilter] ?: "",
                                        dataCount = uiState.dataPoints.size,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                            }
                            
                            // Data points list
                            items(
                                items = uiState.dataPoints,
                                key = { it.id }
                            ) { dataPoint ->
                                SwipeableListItem(
                                    onSwipeToStart = {
                                        viewModel.deleteDataPoint(dataPoint.id)
                                    },
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                ) {
                                    DataPointItem(
                                        dataPoint = dataPoint,
                                        pluginName = uiState.pluginNames[dataPoint.pluginId] ?: "Unknown",
                                        isSelected = uiState.selectedDataPoints.contains(dataPoint.id),
                                        isInSelectionMode = uiState.isInSelectionMode,
                                        onClick = {
                                            if (uiState.isInSelectionMode) {
                                                viewModel.toggleDataPointSelection(dataPoint.id)
                                            } else {
                                                navController.navigate("data_detail/${dataPoint.id}")
                                            }
                                        },
                                        onLongClick = {
                                            viewModel.toggleDataPointSelection(dataPoint.id)
                                        },
                                        onDelete = {
                                            viewModel.deleteDataPoint(dataPoint.id)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Deletion loading overlay
                if (uiState.isDeleting) {
                    LoadingOverlay(
                        isLoading = true,
                        message = "Deleting...",
                        fullScreen = true
                    )
                }
            }
        }
    }
}

/**
 * Selection mode top bar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionModeTopBar(
    selectedCount: Int,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onDelete: () -> Unit
) {
    TopAppBar(
        title = { Text("$selectedCount selected") },
        navigationIcon = {
            IconButton(onClick = onClearSelection) {
                Icon(Icons.Default.Close, contentDescription = "Clear selection")
            }
        },
        actions = {
            IconButton(onClick = onSelectAll) {
                Icon(Icons.Default.SelectAll, contentDescription = "Select all")
            }
            IconButton(
                onClick = onDelete,
                enabled = selectedCount > 0
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete selected",
                    tint = if (selectedCount > 0) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    )
}

/**
 * Data summary card
 */
@Composable
private fun DataSummaryCard(
    title: String,
    dataCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "$dataCount entries",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
            Icon(
                imageVector = AppIcons.Data.analytics,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

/**
 * Individual data point item
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DataPointItem(
    dataPoint: Any, // This would be your actual DataPoint type
    pluginName: String,
    isSelected: Boolean,
    isInSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
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
                    text = pluginName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                // This would show actual data content
                Text(
                    text = "Data content here",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                        .format(Date()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            
            if (isInSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() }
                )
            } else {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
