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
    plugins: List<Plugin>,
    onDismiss: () -> Unit,
    onPluginSelected: (Plugin) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Quick Add",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(16.dp)
            )
            
            Divider()
            
            LazyColumn {
                items(plugins) { plugin ->
                    QuickAddItem(
                        plugin = plugin,
                        onClick = {
                            scope.launch {
                                sheetState.hide()
                                onPluginSelected(plugin)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun QuickAddItem(
    plugin: Plugin,
    onClick: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    
    ListItem(
        headlineContent = { Text(plugin.metadata.name) },
        supportingContent = { Text(plugin.metadata.description) },
        modifier = Modifier.clickable { 
            when (plugin.id) {
                "water" -> showDialog = true
                "counter" -> onClick()
                else -> onClick()
            }
        }
    )
    
    // Custom dialogs for specific plugins
    when (plugin.id) {
        "water" -> {
            if (showDialog) {
                WaterQuickAddDialog(
                    onDismiss = { showDialog = false },
                    onConfirm = { amount ->
                        showDialog = false
                        onClick()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterQuickAddDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var amount by remember { mutableStateOf("250") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Water") },
        text = {
            Column {
                Text("How much water did you drink?")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { char -> char.isDigit() } },
                    label = { Text("Amount (ml)") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = amount == "250",
                        onClick = { amount = "250" },
                        label = { Text("250ml") }
                    )
                    FilterChip(
                        selected = amount == "500",
                        onClick = { amount = "500" },
                        label = { Text("500ml") }
                    )
                    FilterChip(
                        selected = amount == "1000",
                        onClick = { amount = "1000" },
                        label = { Text("1L") }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(amount.toIntOrNull() ?: 250) }
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
