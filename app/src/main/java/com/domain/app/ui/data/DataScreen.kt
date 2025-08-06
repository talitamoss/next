// app/src/main/java/com/domain/app/ui/data/DataScreen.kt
import androidx.compose.foundation.BorderStroke
package com.domain.app.ui.data

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.domain.app.core.data.DataPoint
import com.domain.app.ui.theme.AppIcons
import com.domain.app.ui.utils.getPluginIconById
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DataScreen(
    navController: NavController,
    viewModel: DataViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    
    // Snackbar state
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Show messages
    LaunchedEffect(uiState.message, uiState.error) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            viewModel.clearMessage()
        }
        uiState.error?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Long
            )
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
                        viewModel.deleteMultipleDataPoints(uiState.selectedDataPoints.toList())
                    }
                )
            } else {
                TopAppBar(
                    title = { Text("Data Management") },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(
                                imageVector = AppIcons.Navigation.back,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        // Search button
                        IconButton(onClick = { /* TODO: Implement search */ }) {
                            Icon(
                                imageVector = AppIcons.Action.search,
                                contentDescription = "Search"
                            )
                        }
                        
                        // Filter button
                        var showFilterMenu by remember { mutableStateOf(false) }
                        IconButton(onClick = { showFilterMenu = true }) {
                            Icon(
                                imageVector = AppIcons.Action.filter,
                                contentDescription = "Filter"
                            )
                        }
                        
                        // Filter dropdown menu
                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Plugins") },
                                onClick = {
                                    viewModel.filterByPlugin(null)
                                    showFilterMenu = false
                                },
                                leadingIcon = {
                                    if (uiState.selectedPluginFilter == null) {
                                        Icon(
                                            imageVector = AppIcons.Action.check,
                                            contentDescription = null
                                        )
                                    }
                                }
                            )
                            
                            Divider()
                            
                            uiState.pluginSummaries.forEach { (pluginId, pluginName) ->
                                DropdownMenuItem(
                                    text = { Text(pluginName) },
                                    onClick = {
                                        viewModel.filterByPlugin(pluginId)
                                        showFilterMenu = false
                                    },
                                    leadingIcon = {
                                        if (uiState.selectedPluginFilter == pluginId) {
                                            Icon(
                                                imageVector = AppIcons.Action.check,
                                                contentDescription = null
                                            )
                                        } else {
                                            Icon(
                                                imageVector = getPluginIconById(pluginId),
                                                contentDescription = null
                                            )
                                        }
                                    }
                                )
                            }
                        }
                        
                        // Refresh button
                        IconButton(onClick = { viewModel.refreshData() }) {
                            Icon(
                                imageVector = AppIcons.Action.refresh,
                                contentDescription = "Refresh"
                            )
                        }
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    // Loading state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                
                uiState.dataPoints.isEmpty() -> {
                    // Empty state
                    EmptyDataState(
                        selectedFilter = uiState.selectedPluginFilter,
                        pluginName = uiState.pluginNames[uiState.selectedPluginFilter]
                    )
                }
                
                else -> {
                    // Data list
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Summary card
                        item {
                            DataSummaryCard(
                                totalCount = uiState.dataPoints.size,
                                selectedFilter = uiState.selectedPluginFilter,
                                pluginName = uiState.pluginNames[uiState.selectedPluginFilter]
                            )
                        }
                        
                        // Data items
                        items(
                            items = uiState.dataPoints,
                            key = { it.id }
                        ) { dataPoint ->
                            DataPointItem(
                                dataPoint = dataPoint,
                                pluginName = uiState.pluginNames[dataPoint.pluginId] ?: "Unknown",
                                isSelected = uiState.selectedDataPoints.contains(dataPoint.id),
                                isInSelectionMode = uiState.isInSelectionMode,
                                onClick = {
                                    if (uiState.isInSelectionMode) {
                                        viewModel.toggleDataPointSelection(dataPoint.id)
                                    } else {
                                        // Navigate to detail or edit
                                        navController.navigate("data_detail/${dataPoint.id}")
                                    }
                                },
                                onLongClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
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
            
            // Loading overlay for delete operations
            if (uiState.isDeleting) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card {
                        Box(
                            modifier = Modifier.padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator()
                                Text("Deleting...")
                            }
                        }
                    }
                }
            }
        }
    }
}

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
                Icon(
                    imageVector = AppIcons.Action.close,
                    contentDescription = "Clear selection"
                )
            }
        },
        actions = {
            TextButton(onClick = onSelectAll) {
                Text("Select All")
            }
            
            IconButton(
                onClick = onDelete,
                enabled = selectedCount > 0
            ) {
                Icon(
                    imageVector = AppIcons.Action.delete,
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
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    )
}

@Composable
private fun DataSummaryCard(
    totalCount: Int,
    selectedFilter: String?,
    pluginName: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
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
                    text = if (selectedFilter != null) {
                        "$pluginName Data"
                    } else {
                        "All Data"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$totalCount entries",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
            
            Icon(
                imageVector = if (selectedFilter != null) {
                    getPluginIconById(selectedFilter)
                } else {
                    AppIcons.Data.analytics
                },
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DataPointItem(
    dataPoint: DataPoint,
    pluginName: String,
    isSelected: Boolean,
    isInSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Selection checkbox (if in selection mode)
            if (isInSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            // Plugin icon
            Icon(
                imageVector = getPluginIconById(dataPoint.pluginId),
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(8.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            // Data content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Plugin name and timestamp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = pluginName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = dateFormatter.format(dataPoint.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Data values
                dataPoint.value.entries.take(3).forEach { (key, value) ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${key.replaceFirstChar { it.uppercase() }}:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = value.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                if (dataPoint.value.size > 3) {
                    Text(
                        text = "+${dataPoint.value.size - 3} more fields",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // Delete button (if not in selection mode)
            if (!isInSelectionMode) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.padding(0.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.Action.delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyDataState(
    selectedFilter: String?,
    pluginName: String?
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = AppIcons.Data.analytics,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            
            Text(
                text = if (selectedFilter != null) {
                    "No data for $pluginName"
                } else {
                    "No data collected yet"
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = if (selectedFilter != null) {
                    "Start collecting data with this plugin"
                } else {
                    "Enable plugins to start collecting data"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
