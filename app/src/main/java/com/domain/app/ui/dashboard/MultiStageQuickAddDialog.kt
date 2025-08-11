package com.domain.app.ui.dashboard

import com.domain.app.core.validation.ValidationResult
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.domain.app.core.data.DataPoint
import com.domain.app.core.plugin.InputType
import com.domain.app.core.plugin.Plugin
import com.domain.app.core.plugin.ValidationResult
import com.domain.app.ui.components.core.feedback.LoadingButton
import com.domain.app.ui.components.core.input.ValidatedTextField
import com.domain.app.ui.theme.AppIcons
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

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
                        text = "Quick Add: ${plugin.metadata.name}",
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
                            label = plugin.metadata.inputType.name.lowercase().replace('_', ' '),
                            placeholder = "Enter ${plugin.metadata.inputType.name.lowercase().replace('_', ' ')}",
                            validator = { value ->
                                when (plugin.metadata.inputType) {
                                    InputType.NUMBER -> {
                                        if (value.toDoubleOrNull() == null) {
                                            ValidationResult.Error("Please enter a valid number")
                                        } else {
                                            ValidationResult.Success
                                        }
                                    }
                                    InputType.TEXT -> {
                                        if (value.isEmpty()) {
                                            ValidationResult.Error("This field is required")
                                        } else {
                                            ValidationResult.Success
                                        }
                                    }
                                    InputType.SCALE -> {
                                        val number = value.toIntOrNull()
                                        if (number == null) {
                                            ValidationResult.Error("Please enter a number")
                                        } else if (number !in 1..10) {
                                            ValidationResult.Error("Please enter a value between 1 and 10")
                                        } else {
                                            ValidationResult.Success
                                        }
                                    }
                                    InputType.CHOICE -> {
                                        if (value.isEmpty()) {
                                            ValidationResult.Error("Please select an option")
                                        } else {
                                            ValidationResult.Success
                                        }
                                    }
                                    InputType.SLIDER -> {
                                        val number = value.toDoubleOrNull()
                                        if (number == null) {
                                            ValidationResult.Error("Please enter a valid number")
                                        } else {
                                            ValidationResult.Success
                                        }
                                    }
                                    InputType.DURATION -> {
                                        val duration = value.toLongOrNull()
                                        if (duration == null || duration < 0) {
                                            ValidationResult.Error("Please enter a valid duration")
                                        } else {
                                            ValidationResult.Success
                                        }
                                    }
                                    InputType.TIME_PICKER -> {
                                        if (value.isEmpty()) {
                                            ValidationResult.Error("Please select a time")
                                        } else {
                                            ValidationResult.Success
                                        }
                                    }
                                    InputType.DATE_PICKER -> {
                                        if (value.isEmpty()) {
                                            ValidationResult.Error("Please select a date")
                                        } else {
                                            ValidationResult.Success
                                        }
                                    }
                                }
                            }
                        )
                    }
                    
                    1 -> {
                        // Notes input stage
                        Text(
                            text = "Add notes (optional)",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Notes") },
                            placeholder = { Text("Any additional context...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            maxLines = 4
                        )
                    }
                }
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (currentStage > 0) {
                        TextButton(
                            onClick = { currentStage-- },
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading
                        ) {
                            Text("Back")
                        }
                    } else {
                        TextButton(
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
                                // Validate before moving to next stage
                                if (inputValue.isNotEmpty()) {
                                    currentStage++
                                }
                            } else {
                                scope.launch {
                                    isLoading = true
                                    
                                    // Create data point with proper structure
                                    val dataPoint = DataPoint(
                                        id = UUID.randomUUID().toString(),
                                        pluginId = plugin.id,
                                        timestamp = Instant.now(),
                                        type = "manual",
                                        value = buildMap {
                                            put("value", inputValue)
                                            if (notes.isNotEmpty()) {
                                                put("notes", notes)
                                            }
                                        },
                                        metadata = emptyMap(),
                                        source = "manual_entry"
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
