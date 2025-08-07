package com.domain.app.ui.data

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.domain.app.core.storage.entity.DataPoint
import com.domain.app.ui.components.core.lists.SwipeableDataItem
import com.domain.app.ui.theme.AppIcons
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataScreen(
    onNavigateBack: () -> Unit,
    viewModel: DataViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    var selectedFilters by remember { mutableStateOf(emptySet<String>()) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Data Management") },
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
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Summary card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    DataSummaryItem(
                        label = "Total Points",
                        value = uiState.totalDataPoints.toString()
                    )
                    DataSummaryItem(
                        label = "Plugins",
                        value = uiState.activePluginCount.toString()
                    )
                    DataSummaryItem(
                        label = "This Week",
                        value = uiState.weeklyDataPoints.toString()
                    )
                }
            }
            
            // Filter chips
            if (selectedFilters.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    selectedFilters.forEach { filter ->
                        FilterChip(
                            selected = true,
                            onClick = {
                                selectedFilters = selectedFilters - filter
                                viewModel.updateFilters(selectedFilters.toList())
                            },
                            label = { Text(filter) },
                            trailingIcon = {
                                Icon(
                                    imageVector = AppIcons.Navigation.close,
                                    contentDescription = "Remove filter",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }
            
            // Data list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = uiState.dataPoints,
                    key = { it.id }
                ) { dataPoint ->
                    SwipeableDataItem(
                        title = dataPoint.pluginId,
                        subtitle = formatTimestamp(dataPoint.timestamp),
                        value = dataPoint.value,
                        icon = AppIcons.Plugin.custom,
                        onDelete = {
                            viewModel.deleteDataPoint(dataPoint)
                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "Data point deleted",
                                    actionLabel = "Undo",
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.restoreDataPoint(dataPoint)
                                }
                            }
                        },
                        onEdit = {
                            // Navigate to edit screen
                        }
                    )
                }
                
                if (uiState.dataPoints.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = AppIcons.Storage.database,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "No data points yet",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Filter dialog
    if (showFilterDialog) {
        FilterDialog(
            availableFilters = uiState.availablePlugins,
            selectedFilters = selectedFilters,
            onFiltersSelected = { filters ->
                selectedFilters = filters
                viewModel.updateFilters(filters.toList())
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
                scope.launch {
                    snackbarHostState.showSnackbar("Data exported successfully")
                }
            },
            onDismiss = { showExportDialog = false }
        )
    }
}

@Composable
private fun DataSummaryItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FilterDialog(
    availableFilters: List<String>,
    selectedFilters: Set<String>,
    onFiltersSelected: (Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var tempSelection by remember { mutableStateOf(selectedFilters) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter Data") },
        text = {
            Column {
                Text(
                    text = "Select plugins to filter",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                availableFilters.forEach { filter ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = filter in tempSelection,
                            onCheckedChange = { checked ->
                                tempSelection = if (checked) {
                                    tempSelection + filter
                                } else {
                                    tempSelection - filter
                                }
                            }
                        )
                        Text(
                            text = filter,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onFiltersSelected(tempSelection) }
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
    var selectedFormat by remember { mutableStateOf(ExportFormat.JSON) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Data") },
        text = {
            Column {
                Text(
                    text = "Select export format",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                ExportFormat.values().forEach { format ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = format == selectedFormat,
                            onClick = { selectedFormat = format }
                        )
                        Text(
                            text = format.displayName,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onExport(selectedFormat) }
            ) {
                Text("Export")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

enum class ExportFormat(val displayName: String) {
    JSON("JSON"),
    CSV("CSV"),
    XML("XML")
}

private fun formatTimestamp(timestamp: Long): String {
    val instant = Instant.ofEpochMilli(timestamp)
    val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
    val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")
    return dateTime.format(formatter)
}
