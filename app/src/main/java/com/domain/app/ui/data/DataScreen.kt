// app/src/main/java/com/domain/app/ui/data/DataScreen.kt
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
    onNavigateToSettings: () -> Unit,  // Changed from onNavigateBack
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
                            text = "Reflect",  // Changed from "Data"
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
                        IconButton(onClick = onNavigateToSettings) {  // Added settings icon
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
                    value = uiState.weeklyDataPoints.size.toString(),  // FIXED: Changed from thisWeekCount
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
    
    // Dialogs remain the same...
}

// Private composable components that were missing:

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

// Add other private composables here (DataPointCard, FilterDialog, ExportDialog, etc.)
// These should already exist in your file - just making sure they're included
