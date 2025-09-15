// app/src/main/java/com/domain/app/ui/data/DataScreen.kt
package com.domain.app.ui.data

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.domain.app.core.data.DataPoint
import com.domain.app.core.plugin.ExportFormat
import com.domain.app.core.plugin.Plugin
import com.domain.app.ui.theme.AppIcons
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings"
                            )
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SummaryCard(
                        title = "Total Records",
                        value = uiState.dataPoints.size.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        title = "This Week",
                        value = uiState.weeklyDataPoints.size.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // Plugin Filter Chips
            if (uiState.plugins.isNotEmpty()) {
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = uiState.selectedPluginFilter == null,
                                onClick = { viewModel.filterByPlugin(null) },
                                label = { Text("All") }
                            )
                        }
                        items(uiState.plugins) { plugin ->
                            FilterChip(
                                selected = uiState.selectedPluginFilter == plugin.id,
                                onClick = { viewModel.filterByPlugin(plugin.id) },
                                label = { Text(plugin.metadata.name) }
                            )
                        }
                    }
                }
            }
            
            // Data Points
            val filteredDataPoints = uiState.dataPoints.filter { dataPoint ->
                val matchesPlugin = uiState.selectedPluginFilter == null || 
                    dataPoint.pluginId == uiState.selectedPluginFilter
                val matchesSearch = uiState.searchQuery.isEmpty() || 
                    dataPoint.value.toString().contains(uiState.searchQuery, ignoreCase = true)
                matchesPlugin && matchesSearch
            }
            
            if (filteredDataPoints.isEmpty()) {
                item {
                    EmptyState(
                        message = when {
                            uiState.dataPoints.isEmpty() -> "No data recorded yet"
                            uiState.selectedPluginFilter != null -> "No data for this tracker"
                            uiState.searchQuery.isNotEmpty() -> "No results found"
                            else -> "No data available"
                        }
                    )
                }
            } else {
                items(filteredDataPoints, key = { it.id }) { dataPoint ->
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
    
    // Dialogs
    if (showFilterDialog) {
        FilterDialog(
            onDismiss = { showFilterDialog = false },
            onApplyFilters = { filterState ->
                viewModel.updateFilters(filterState)
                showFilterDialog = false
            },
            currentFilters = FilterState()
        )
    }
    
    if (showExportDialog) {
        EnhancedExportDialog(
            onDismiss = { showExportDialog = false },
            onExport = { options ->
                viewModel.exportData(options)
                showExportDialog = false
            },
            availablePlugins = uiState.plugins
        )
    }
    
    // Show messages
    uiState.message?.let { message ->
        LaunchedEffect(message) {
            // Show snackbar or toast
            viewModel.clearMessage()
        }
    }
    
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Show error snackbar or toast
            viewModel.clearError()
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
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
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
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pluginName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = LocalDateTime.ofInstant(dataPoint.timestamp, ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("MMM dd, yyyy • HH:mm")),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (!isInSelectionMode) {
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
                style = MaterialTheme.typography.bodyLarge
            )
            
            // Expanded details
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
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
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionModeTopBar(
    selectedCount: Int,
    onCancelSelection: () -> Unit,
    onDeleteSelected: () -> Unit
) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onCancelSelection) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel selection"
                )
            }
        },
        title = {
            Text("$selectedCount selected")
        },
        actions = {
            IconButton(
                onClick = onDeleteSelected,
                enabled = selectedCount > 0
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete selected"
                )
            }
        }
    )
}

@Composable
private fun EmptyState(
    message: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = AppIcons.Storage.folder,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnhancedExportDialog(
    onDismiss: () -> Unit,
    onExport: (ExportOptions) -> Unit,
    availablePlugins: List<Plugin>
) {
    var selectedFormat by remember { mutableStateOf(ExportFormat.CSV) }
    var selectedTimeFrame by remember { mutableStateOf(TimeFrame.ALL) }
    var selectedPlugins by remember { mutableStateOf(availablePlugins.map { it.id }.toSet()) }
    var customStartDate by remember { mutableStateOf<LocalDateTime?>(null) }
    var customEndDate by remember { mutableStateOf<LocalDateTime?>(null) }
    var encryptExport by remember { mutableStateOf(false) }
    var showFormatDropdown by remember { mutableStateOf(false) }
    var showTimeFrameDropdown by remember { mutableStateOf(false) }
    var showPluginSelector by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Data") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Format Dropdown
                Column {
                    Text(
                        "Export Format",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box {
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showFormatDropdown = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(selectedFormat.name)
                                    Text(
                                        text = when (selectedFormat) {
                                            ExportFormat.CSV -> "Spreadsheet compatible"
                                            ExportFormat.JSON -> "Developer-friendly"
                                            ExportFormat.XML -> "Structured data"
                                            ExportFormat.CUSTOM -> "Plugin-specific"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null
                                )
                            }
                        }
                        
                        DropdownMenu(
                            expanded = showFormatDropdown,
                            onDismissRequest = { showFormatDropdown = false }
                        ) {
                            ExportFormat.values().forEach { format ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(format.name)
                                            Text(
                                                text = when (format) {
                                                    ExportFormat.CSV -> "Spreadsheet compatible"
                                                    ExportFormat.JSON -> "Developer-friendly"
                                                    ExportFormat.XML -> "Structured data"
                                                    ExportFormat.CUSTOM -> "Plugin-specific"
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedFormat = format
                                        showFormatDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                // Time Frame Dropdown
                Column {
                    Text(
                        "Time Frame",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box {
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showTimeFrameDropdown = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(selectedTimeFrame.displayName)
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null
                                )
                            }
                        }
                        
                        DropdownMenu(
                            expanded = showTimeFrameDropdown,
                            onDismissRequest = { showTimeFrameDropdown = false }
                        ) {
                            TimeFrame.values().forEach { timeFrame ->
                                DropdownMenuItem(
                                    text = { Text(timeFrame.displayName) },
                                    onClick = {
                                        selectedTimeFrame = timeFrame
                                        showTimeFrameDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                // Custom Date Range (if CUSTOM selected)
                AnimatedVisibility(visible = selectedTimeFrame == TimeFrame.CUSTOM) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // TODO: Add date pickers for custom range
                        Text(
                            "Custom date range selection coming soon",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Plugin Selection
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Select Trackers",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(
                            onClick = {
                                selectedPlugins = if (selectedPlugins.size == availablePlugins.size) {
                                    emptySet()
                                } else {
                                    availablePlugins.map { it.id }.toSet()
                                }
                            }
                        ) {
                            Text(
                                if (selectedPlugins.size == availablePlugins.size) "Deselect All" else "Select All"
                            )
                        }
                    }
                    
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPluginSelector = !showPluginSelector }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                when {
                                    selectedPlugins.isEmpty() -> "No trackers selected"
                                    selectedPlugins.size == availablePlugins.size -> "All trackers"
                                    selectedPlugins.size == 1 -> {
                                        availablePlugins.find { it.id == selectedPlugins.first() }?.metadata?.name ?: "1 tracker"
                                    }
                                    else -> "${selectedPlugins.size} trackers"
                                }
                            )
                            Icon(
                                imageVector = if (showPluginSelector) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null
                            )
                        }
                    }
                    
                    AnimatedVisibility(visible = showPluginSelector) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                        ) {
                            LazyColumn(
                                modifier = Modifier.padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(availablePlugins) { plugin ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedPlugins = if (plugin.id in selectedPlugins) {
                                                    selectedPlugins - plugin.id
                                                } else {
                                                    selectedPlugins + plugin.id
                                                }
                                            }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = plugin.id in selectedPlugins,
                                            onCheckedChange = { checked ->
                                                selectedPlugins = if (checked) {
                                                    selectedPlugins + plugin.id
                                                } else {
                                                    selectedPlugins - plugin.id
                                                }
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(plugin.metadata.name)
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Encryption Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { encryptExport = !encryptExport },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = encryptExport,
                        onCheckedChange = { encryptExport = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Encrypt export")
                        Text(
                            "Password protect your data",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val options = ExportOptions(
                        format = selectedFormat,
                        timeFrame = selectedTimeFrame,
                        selectedPlugins = selectedPlugins,
                        customDateRange = if (selectedTimeFrame == TimeFrame.CUSTOM && customStartDate != null && customEndDate != null) {
                            customStartDate!!.atZone(ZoneId.systemDefault()).toInstant() to
                            customEndDate!!.atZone(ZoneId.systemDefault()).toInstant()
                        } else null,
                        encrypt = encryptExport
                    )
                    onExport(options)
                    onDismiss()
                },
                enabled = selectedPlugins.isNotEmpty()
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
