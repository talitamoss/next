// app/src/main/java/com/domain/app/ui/data/DataScreen.kt
package com.domain.app.ui.data

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.domain.app.core.data.DataPoint
import com.domain.app.ui.components.core.feedback.LoadingContainer
import com.domain.app.ui.components.core.feedback.NoDataEmptyState
import com.domain.app.ui.theme.AppIcons
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Data screen showing all collected data points with filtering and management options
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataScreen(
    navController: NavController,
    viewModel: DataViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showFilterMenu by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Data") },
                actions = {
                    IconButton(onClick = { showFilterMenu = true }) {
                        Icon(AppIcons.Action.filter, contentDescription = "Filter")
                    }
                    IconButton(onClick = { /* TODO: Date picker */ }) {
                        Icon(AppIcons.Data.calendar, contentDescription = "Calendar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        // Using our new LoadingContainer component
        LoadingContainer(
            isLoading = uiState.isLoading,
            isEmpty = uiState.dataPoints.isEmpty(),
            errorMessage = uiState.error,
            onRetry = { viewModel.refreshData() },
            emptyMessage = "No data recorded yet",
            modifier = Modifier.padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Group data by date - handle different timestamp types
                val groupedData = uiState.dataPoints.groupBy { dataPoint ->
                    when (val timestamp = dataPoint.timestamp) {
                        is LocalDateTime -> timestamp.toLocalDate()
                        is Instant -> timestamp.atZone(ZoneId.systemDefault()).toLocalDate()
                        is Long -> Instant.ofEpochMilli(timestamp)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        else -> LocalDate.now()
                    }
                }
                
                groupedData.forEach { (date, dataPoints) ->
                    item(key = date) {
                        DateHeader(
                            date = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
                        )
                    }
                    
                    items(
                        items = dataPoints,
                        key = { it.id }
                    ) { dataPoint ->
                        // Find plugin name from availablePlugins (which is List<Pair<String, String>>)
                        val pluginInfo = uiState.availablePlugins.find { 
                            it.first == dataPoint.pluginId 
                        }
                        
                        DataPointCard(
                            dataPoint = dataPoint,
                            pluginName = pluginInfo?.second ?: dataPoint.pluginId,
                            onDelete = { viewModel.deleteDataPoint(dataPoint.id) },
                            onEdit = { /* TODO: Edit functionality */ }
                        )
                    }
                }
            }
        }
    }
    
    // Filter dropdown menu
    if (showFilterMenu) {
        FilterDropdownMenu(
            expanded = showFilterMenu,
            onDismissRequest = { showFilterMenu = false },
            selectedPlugin = uiState.selectedPluginFilter,
            availablePlugins = uiState.availablePlugins.map { (id, name) ->
                PluginInfo(
                    id = id,
                    name = name,
                    icon = null // Could add icon support later
                )
            },
            onPluginSelected = { pluginId ->
                viewModel.filterByPlugin(pluginId)
                showFilterMenu = false
            }
        )
    }
}

@Composable
private fun DateHeader(date: String) {
    Text(
        text = date,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DataPointCard(
    dataPoint: DataPoint,
    pluginName: String,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        onClick = onEdit
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
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Display value based on plugin type
                    val displayText = formatDataPointValue(dataPoint)
                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    // Show notes if available
                    dataPoint.value["note"]?.let { note ->
                        if (note.toString().isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = note.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    // Format time based on timestamp type
                    val timeText = when (val timestamp = dataPoint.timestamp) {
                        is LocalDateTime -> timestamp.toLocalTime()
                            .format(DateTimeFormatter.ofPattern("HH:mm"))
                        is Instant -> timestamp.atZone(ZoneId.systemDefault())
                            .toLocalTime()
                            .format(DateTimeFormatter.ofPattern("HH:mm"))
                        is Long -> Instant.ofEpochMilli(timestamp)
                            .atZone(ZoneId.systemDefault())
                            .toLocalTime()
                            .format(DateTimeFormatter.ofPattern("HH:mm"))
                        else -> "00:00"
                    }
                    
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Row {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = AppIcons.Action.edit,
                                contentDescription = "Edit",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        IconButton(
                            onClick = { showDeleteConfirmation = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = AppIcons.Action.delete,
                                contentDescription = "Delete",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
            
            // Tags or metadata
            dataPoint.value["tags"]?.let { tags ->
                if (tags is List<*> && tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        tags.forEach { tag ->
                            AssistChip(
                                onClick = { },
                                label = { Text(tag.toString()) },
                                modifier = Modifier.height(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Entry?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirmation = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun FilterDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    selectedPlugin: String?,
    availablePlugins: List<PluginInfo>,
    onPluginSelected: (String?) -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest
    ) {
        DropdownMenuItem(
            text = { 
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("All Plugins")
                    if (selectedPlugin == null) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Selected",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            onClick = { onPluginSelected(null) }
        )
        
        HorizontalDivider()
        
        availablePlugins.forEach { plugin ->
            DropdownMenuItem(
                text = { 
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(plugin.name)
                        if (selectedPlugin == plugin.id) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Selected",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                onClick = { onPluginSelected(plugin.id) }
            )
        }
    }
}

/**
 * Format data point value for display based on plugin type
 */
private fun formatDataPointValue(dataPoint: DataPoint): String {
    return when (dataPoint.pluginId) {
        "water" -> {
            val amount = dataPoint.value["amount"] as? Number
            val unit = dataPoint.value["unit"] as? String ?: "ml"
            "$amount $unit"
        }
        "counter" -> {
            val count = dataPoint.value["count"] as? Number
            val label = dataPoint.value["label"] as? String ?: ""
            "$count $label"
        }
        "mood" -> {
            val mood = dataPoint.value["mood"] as? Number ?: 3
            val moodEmoji = when (mood.toInt()) {
                1 -> "😢"
                2 -> "😕"
                3 -> "😐"
                4 -> "😊"
                5 -> "😄"
                else -> "😐"
            }
            val note = dataPoint.value["note"] as? String
            if (note.isNullOrBlank()) moodEmoji else "$moodEmoji - $note"
        }
        "sleep" -> {
            val hours = dataPoint.value["hours"] as? Number ?: 0
            val quality = dataPoint.value["quality"] as? Number
            val qualityText = quality?.let { " (Quality: ${it}/5)" } ?: ""
            "${hours}h sleep$qualityText"
        }
        "exercise" -> {
            val type = dataPoint.value["type"] as? String ?: "Exercise"
            val duration = dataPoint.value["duration"] as? Number ?: 0
            "$type for $duration min"
        }
        "energy" -> {
            val level = dataPoint.value["level"] as? Number ?: 3
            val levelText = when (level.toInt()) {
                1 -> "Exhausted"
                2 -> "Tired"
                3 -> "Normal"
                4 -> "Energetic"
                5 -> "Very Energetic"
                else -> "Level $level"
            }
            "Energy: $levelText"
        }
        else -> {
            // Generic formatting for unknown plugins
            dataPoint.value.entries.joinToString(", ") { (key, value) ->
                "$key: $value"
            }
        }
    }
}

// Data class for plugin info
data class PluginInfo(
    val id: String,
    val name: String,
    val icon: String? = null
)
