// app/src/main/java/com/domain/app/ui/data/DataScreen.kt
package com.domain.app.ui.data

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.domain.app.core.plugin.ExportFormat  // IMPORT FROM CORRECT PACKAGE
import com.domain.app.ui.theme.AppIcons
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// DO NOT CREATE FilterState - it exists in DataViewModel
// DO NOT CREATE ExportFormat - it exists in core.plugin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataScreen(
    onNavigateToSettings: () -> Unit,
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
                    title = { 
                        Text(
                            text = "Reflect",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
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
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(
                                imageVector = AppIcons.Navigation.settings,
                                contentDescription = "Settings"
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
                    value = uiState.weeklyDataPoints.size.toString(),
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
    
    // Dialogs
    if (showFilterDialog) {
        FilterDialog(
            onDismiss = { showFilterDialog = false },
            onApplyFilters = { filterState ->
                viewModel.updateFilters(filterState)
                showFilterDialog = false
            },
            currentFilters = FilterState() // Uses FilterState from DataViewModel
        )
    }
    
    if (showExportDialog) {
        ExportDialog(
            onDismiss = { showExportDialog = false },
            onExport = { format ->
                viewModel.exportData(format)
                showExportDialog = false
            }
        )
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
    onLongPress: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
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
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header row with plugin name and menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Plugin badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = pluginName,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                
                // Selection checkbox or menu
                if (isInSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelection() }
                    )
                } else {
                    var showMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options"
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null
                                    )
                                }
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Display main value
            val mainValue = when {
                dataPoint.value.containsKey("value") -> dataPoint.value["value"].toString()
                dataPoint.value.containsKey("amount") -> "${dataPoint.value["amount"]} ml"
                dataPoint.value.containsKey("mood") -> "Mood: ${dataPoint.value["mood"]}"
                dataPoint.value.containsKey("text") -> dataPoint.value["text"].toString()
                else -> dataPoint.value.entries.firstOrNull()?.let { "${it.key}: ${it.value}" } ?: "No data"
            }
            
            Text(
                text = mainValue,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            
            // Timestamp
            val formatter = DateTimeFormatter.ofPattern("MMM d, h:mm a")
            val localDateTime = dataPoint.timestamp.atZone(ZoneId.systemDefault()).toLocalDateTime()
            Text(
                text = localDateTime.format(formatter),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Expandable details
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // Show all data fields
                    dataPoint.value.forEach { (key, value) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = key.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = value.toString(),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    // Show metadata if exists - FIX: Extract before @Composable calls
                    val metadataEntries = dataPoint.metadata?.entries?.toList() ?: emptyList()
                    if (metadataEntries.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Metadata",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        metadataEntries.forEach { entry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = entry.key,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = entry.value,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterDialog(
    onDismiss: () -> Unit,
    onApplyFilters: (FilterState) -> Unit,
    currentFilters: FilterState
) {
    // Placeholder implementation
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filters") },
        text = { 
            Column {
                Text("Filter functionality coming soon")
                Text("- Filter by plugin", style = MaterialTheme.typography.bodySmall)
                Text("- Filter by date range", style = MaterialTheme.typography.bodySmall)
                Text("- Search in values", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

@Composable
private fun ExportDialog(
    onDismiss: () -> Unit,
    onExport: (ExportFormat) -> Unit
) {
    var selectedFormat by remember { mutableStateOf(ExportFormat.CSV) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Data") },
        text = {
            Column {
                Text("Select export format:")
                Spacer(modifier = Modifier.height(16.dp))
                
                ExportFormat.values().forEach { format ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedFormat = format }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedFormat == format,
                            onClick = { selectedFormat = format }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(format.name)
                            val description = when (format) {
                                ExportFormat.CSV -> "Spreadsheet compatible format"
                                ExportFormat.JSON -> "Developer-friendly format"
                                ExportFormat.XML -> "Structured data format"
                                ExportFormat.CUSTOM -> "Plugin-specific format"
                            }
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onExport(selectedFormat)
                    onDismiss()
                }
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
