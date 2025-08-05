package com.domain.app.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.domain.app.core.plugin.Plugin

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
