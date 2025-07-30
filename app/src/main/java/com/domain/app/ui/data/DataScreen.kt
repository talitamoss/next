// app/src/main/java/com/domain/app/ui/data/DataScreen.kt
package com.domain.app.ui.data

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import com.domain.app.ui.theme.AppIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.domain.app.core.data.DataPoint
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

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
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.dataPoints.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No data recorded yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Group data by day
                    val groupedData = uiState.dataPoints.groupBy { dataPoint ->
                        dataPoint.timestamp.atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                    }
                    
                    groupedData.forEach { (date, dataPoints) ->
                        item {
                            Text(
                                text = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        
                        items(dataPoints) { dataPoint ->
                            DataPointCard(
                                dataPoint = dataPoint,
                                pluginName = uiState.pluginNames[dataPoint.pluginId] ?: dataPoint.pluginId
                            )
                        }
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
            availablePlugins = uiState.availablePlugins,
            onPluginSelected = { pluginId ->
                viewModel.filterByPlugin(pluginId)
                showFilterMenu = false
            }
        )
    }
}

@Composable
fun DataPointCard(
    dataPoint: DataPoint,
    pluginName: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
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
                    val displayText = when (dataPoint.pluginId) {
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
                            val note = dataPoint.value["note"] as? String
                            buildString {
                                append("Mood: $mood/5")
                                if (!note.isNullOrBlank()) {
                                    append("\n$note")
                                }
                            }
                        }
                        else -> dataPoint.value.toString()
                    }
                    
                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                Text(
                    text = dataPoint.timestamp
                        .atZone(java.time.ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("HH:mm")),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Show metadata if present
            dataPoint.metadata?.let { metadata ->
                if (metadata.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    metadata.forEach { (key, value) ->
                        if (key != "note") { // Already displayed note above
                            Text(
                                text = "$key: $value",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    selectedPlugin: String?,
    availablePlugins: List<Pair<String, String>>,
    onPluginSelected: (String?) -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest
    ) {
        DropdownMenuItem(
            text = { Text("All Plugins") },
            onClick = { onPluginSelected(null) },
            leadingIcon = if (selectedPlugin == null) {
                { Icon(Icons.Default.Check, contentDescription = "Selected") }
            } else null
        )
        
        availablePlugins.forEach { (pluginId, pluginName) ->
            DropdownMenuItem(
                text = { Text(pluginName) },
                onClick = { onPluginSelected(pluginId) },
                leadingIcon = if (selectedPlugin == pluginId) {
                    { Icon(Icons.Default.Check, contentDescription = "Selected") }
                } else null
            )
        }
    }
}
