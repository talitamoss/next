package com.domain.app.ui.data

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.domain.app.core.data.DataPoint
import com.domain.app.core.plugin.ExportFormat
import com.domain.app.ui.theme.AppIcons
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataScreen(
    onNavigateBack: () -> Unit,
    viewModel: DataViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFilterDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var expandedDataPointId by remember { mutableStateOf<String?>(null) }
    
    Scaffold(
        topBar = {
            if (uiState.isInSelectionMode) {
                SelectionModeTopBar(
                    selectedCount = uiState.selectedDataPoints.size,
                    onCancelSelection = { viewModel.exitSelectionMode() },
                    onDeleteSelected = { viewModel.deleteSelectedDataPoints() }
                )
            } else {
                TopAppBar(
                    title = { Text("Data") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = AppIcons.Navigation.back,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { showFilterDialog = true }) {
                            Icon(
                                imageVector = AppIcons.Action.filter,
                                contentDescription = "Filter"
                            )
                        }
                        IconButton(onClick = { showExportDialog = true }) {
                            Icon(
                                imageVector = AppIcons.Data.upload,
                                contentDescription = "Export"
                            )
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Summary cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryCard(
                    title = "Total",
                    value = uiState.dataPoints.size.toString(),
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "Plugins",
                    value = uiState.plugins.size.toString(),
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "This Week",
                    value = uiState.weeklyDataPoints.size.toString(),  // Fixed - use .size not .toString()
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Data list
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.dataPoints.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No data points yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = uiState.dataPoints,
                        key = { it.id }
                    ) { dataPoint ->
                        DataPointCard(
                            dataPoint = dataPoint,
                            pluginName = uiState.pluginNames[dataPoint.pluginId] ?: dataPoint.pluginId,
                            isExpanded = expandedDataPointId == dataPoint.id,
                            isSelected = uiState.selectedDataPoints.contains(dataPoint.id),
                            isInSelectionMode = uiState.isInSelectionMode,
                            onToggleExpanded = {
                                expandedDataPointId = if (expandedDataPointId == dataPoint.id) null else dataPoint.id
                            },
                            onToggleSelection = {
                                viewModel.toggleDataPointSelection(dataPoint.id)
                            },
                            onDelete = {
                                viewModel.deleteDataPoint(dataPoint.id)
                            },
                            onLongPress = {
                                viewModel.enterSelectionMode()
                                viewModel.toggleDataPointSelection(dataPoint.id)
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Filter dialog
    if (showFilterDialog) {
        FilterDialog(
            plugins = uiState.plugins,
            currentFilter = FilterState(
                selectedPlugin = uiState.plugins.find { it.id == uiState.selectedPluginFilter },  // Fixed - convert String? to Plugin?
                searchQuery = uiState.searchQuery
            ),
            onApply = { filterState ->
                viewModel.updateFilters(filterState)
                showFilterDialog = false
            },
            onDismiss = { showFilterDialog = false }
        )
    }
    
    // Export dialog
    if (showExportDialog) {
        ExportDialog(
            onExport = { format ->
                viewModel.exportData(format)
                showExportDialog = false
            },
            onDismiss = { showExportDialog = false }
        )
    }
    
    // Show snackbar for messages
    uiState.message?.let { message ->
        LaunchedEffect(message) {
            // Show snackbar
            viewModel.clearMessage()
        }
    }
    
    // Show error dialog
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DataPointCard(
    dataPoint: DataPoint,
    pluginName: String,
    isExpanded: Boolean,
    isSelected: Boolean,
    isInSelectionMode: Boolean,
    onToggleExpanded: () -> Unit,
    onToggleSelection: () -> Unit,
    onDelete: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .combinedClickable(
                onClick = {
                    if (isInSelectionMode) {
                        onToggleSelection()
                    } else {
                        onToggleExpanded()
                    }
                },
                onLongClick = onLongPress
            ),
        colors = if (isSelected) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pluginName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")
                            .withZone(ZoneId.systemDefault())
                            .format(dataPoint.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (isInSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelection() }
                    )
                } else {
                    IconButton(onClick = onToggleExpanded) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options"
                        )
                    }
                }
            }
            
            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                
                // Show data values
                dataPoint.value.forEach { (key, value) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = key,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = value.toString(),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                
                // Action buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete")
                    }
                }
            }
        }
    }
}

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
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionModeTopBar(
    selectedCount: Int,
    onCancelSelection: () -> Unit,
    onDeleteSelected: () -> Unit
) {
    TopAppBar(
        title = { Text("$selectedCount selected") },
        navigationIcon = {
            IconButton(onClick = onCancelSelection) {
                Icon(
                    imageVector = AppIcons.Navigation.close,
                    contentDescription = "Cancel selection"
                )
            }
        },
        actions = {
            IconButton(onClick = onDeleteSelected) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete selected"
                )
            }
        }
    )
}

@Composable
private fun FilterDialog(
    plugins: List<com.domain.app.core.plugin.Plugin>,
    currentFilter: FilterState,
    onApply: (FilterState) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedPlugin by remember { mutableStateOf(currentFilter.selectedPlugin) }
    var searchQuery by remember { mutableStateOf(currentFilter.searchQuery) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter Data") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Plugin filter
                Text("Filter by plugin:")
                
                // Plugin selection
                plugins.forEach { plugin ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedPlugin?.id == plugin.id,
                            onClick = { selectedPlugin = plugin }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(plugin.metadata.name)
                    }
                }
                
                // Clear filter option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedPlugin == null,
                        onClick = { selectedPlugin = null }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("All plugins")
                }
                
                // Search query
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onApply(FilterState(selectedPlugin, null, searchQuery))
                }
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ExportDialog(
    onExport: (ExportFormat) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Data") },
        text = {
            Column {
                Text("Select export format:")
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(
                    onClick = { onExport(ExportFormat.CSV) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("CSV")
                }
                
                TextButton(
                    onClick = { onExport(ExportFormat.JSON) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("JSON")
                }
                
                TextButton(
                    onClick = { onExport(ExportFormat.XML) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("XML")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
