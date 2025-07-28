// app/src/main/java/com/domain/app/ui/dashboard/QuickAddBottomSheet.kt
package com.domain.app.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.domain.app.core.plugin.Plugin
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddBottomSheet(
    plugin: Plugin?,
    onDismiss: () -> Unit,
    onDataSubmit: (Plugin, Map<String, Any>) -> Unit
) {
    if (plugin == null) return
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var showPluginDialog by remember { mutableStateOf(false) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Quick Add",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Show plugin info
            ListItem(
                headlineContent = { Text(plugin.metadata.name) },
                supportingContent = { Text(plugin.metadata.description) },
                leadingContent = {
                    // Plugin icon placeholder
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = when(plugin.id) {
                                    "water" -> "💧"
                                    "mood" -> "😊"
                                    else -> plugin.metadata.name.first().toString()
                                },
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                },
                modifier = Modifier.clickable { 
                    showPluginDialog = true 
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Quick action button
            Button(
                onClick = { showPluginDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add ${plugin.metadata.name}")
            }
        }
    }
    
    // Plugin-specific dialogs
    if (showPluginDialog) {
        when (plugin.id) {
            "water" -> {
                val config = plugin.getQuickAddConfig()
                if (config != null) {
                    WaterQuickAddDialog(
                        options = config.options ?: emptyList(),
                        onDismiss = { showPluginDialog = false },
                        onConfirm = { amount ->
                            onDataSubmit(plugin, mapOf("amount" to amount))
                            showPluginDialog = false
                            scope.launch {
                                sheetState.hide()
                                onDismiss()
                            }
                        }
                    )
                }
            }
            "mood" -> {
                val config = plugin.getQuickAddConfig()
                if (config != null) {
                    MoodQuickAddDialog(
                        options = config.options ?: emptyList(),
                        onDismiss = { showPluginDialog = false },
                        onConfirm = { moodValue, note ->
                            val data = mutableMapOf<String, Any>(
                                "value" to moodValue
                            )
                            note?.let { data["note"] = it }
                            onDataSubmit(plugin, data)
                            showPluginDialog = false
                            scope.launch {
                                sheetState.hide()
                                onDismiss()
                            }
                        }
                    )
                }
            }
            // Add other plugin-specific dialogs here
            else -> {
                // Generic input dialog for other plugins
                GenericQuickAddDialog(
                    plugin = plugin,
                    onDismiss = { showPluginDialog = false },
                    onConfirm = { data ->
                        onDataSubmit(plugin, data)
                        showPluginDialog = false
                        scope.launch {
                            sheetState.hide()
                            onDismiss()
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenericQuickAddDialog(
    plugin: Plugin,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, Any>) -> Unit
) {
    var inputValue by remember { mutableStateOf("") }
    val config = plugin.getQuickAddConfig()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add ${plugin.metadata.name}") },
        text = {
            Column {
                Text("Enter value:")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = inputValue,
                    onValueChange = { inputValue = it },
                    label = { Text(config?.title ?: "Value") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val data = mutableMapOf<String, Any>()
                    data["value"] = inputValue
                    onConfirm(data)
                },
                enabled = inputValue.isNotEmpty()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
