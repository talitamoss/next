package com.domain.app.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.domain.app.core.storage.entity.DataPoint
import com.domain.app.core.plugin.Plugin
import com.domain.app.ui.components.core.feedback.LoadingButton
import com.domain.app.ui.components.core.input.ValidatedTextField
import com.domain.app.ui.theme.AppIcons
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiStageQuickAddDialog(
    plugin: Plugin,
    onDismiss: () -> Unit,
    onConfirm: (DataPoint) -> Unit
) {
    var currentStage by remember { mutableStateOf(0) }
    var inputValue by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = !isLoading,
            dismissOnClickOutside = !isLoading
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.Action.add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Quick Add: ${plugin.name}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Progress indicator
                LinearProgressIndicator(
                    progress = { (currentStage + 1) / 2f },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Content based on stage
                when (currentStage) {
                    0 -> {
                        // Value input stage
                        Text(
                            text = "Enter value",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        
                        ValidatedTextField(
                            value = inputValue,
                            onValueChange = { inputValue = it },
                            label = plugin.dataType.displayName,
                            placeholder = "Enter ${plugin.dataType.displayName.lowercase()}",
                            validator = { value ->
                                when (plugin.dataType) {
                                    Plugin.DataType.NUMERIC -> {
                                        if (value.toDoubleOrNull() == null) {
                                            ValidationResult.Error("Please enter a valid number")
                                        } else {
                                            ValidationResult.Valid
                                        }
                                    }
                                    else -> {
                                        if (value.isEmpty()) {
                                            ValidationResult.Error("This field is required")
                                        } else {
                                            ValidationResult.Valid
                                        }
                                    }
                                }
                            }
                        )
                    }
                    
                    1 -> {
                        // Notes stage
                        Text(
                            text = "Add notes (optional)",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Notes") },
                            placeholder = { Text("Any additional context...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5
                        )
                    }
                }
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (currentStage > 0) {
                        OutlinedButton(
                            onClick = { currentStage-- },
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading
                        ) {
                            Text("Back")
                        }
                    } else {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading
                        ) {
                            Text("Cancel")
                        }
                    }
                    
                    LoadingButton(
                        onClick = {
                            if (currentStage < 1) {
                                currentStage++
                            } else {
                                scope.launch {
                                    isLoading = true
                                    // Create data point
                                    val dataPoint = DataPoint(
                                        pluginId = plugin.id,
                                        value = inputValue,
                                        timestamp = Instant.now().toEpochMilli(),
                                        metadata = if (notes.isNotEmpty()) {
                                            mapOf("notes" to notes)
                                        } else {
                                            emptyMap()
                                        }
                                    )
                                    
                                    // Simulate processing
                                    delay(500)
                                    
                                    onConfirm(dataPoint)
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        loading = isLoading,
                        text = if (currentStage < 1) "Next" else "Save",
                        loadingText = "Saving..."
                    )
                }
            }
        }
    }
}

// ValidationResult class for the ValidatedTextField
sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Error(val message: String) : ValidationResult()
    data class Warning(val message: String) : ValidationResult()
}
