package com.domain.app.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.domain.app.core.plugin.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiStageQuickAddDialog(
    plugin: Plugin,
    stages: List<QuickAddStage>,
    onDismiss: () -> Unit,
    onComplete: (Map<String, Any>) -> Unit
) {
    var currentStageIndex by remember { mutableStateOf(0) }
    val collectedData = remember { mutableStateMapOf<String, Any>() }
    val currentStage = stages.getOrNull(currentStageIndex)
    
    if (currentStage == null) {
        onComplete(collectedData.toMap())
        return
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text("${plugin.metadata.name} - Step ${currentStageIndex + 1} of ${stages.size}")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(currentStage.title)
                
                when (currentStage.inputType) {
                    InputType.TEXT -> {
                        var textValue by remember(currentStageIndex) { 
                            mutableStateOf(currentStage.defaultValue as? String ?: "") 
                        }
                        OutlinedTextField(
                            value = textValue,
                            onValueChange = { textValue = it },
                            label = { Text(currentStage.hint ?: "Enter value") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = false,
                            maxLines = 3
                        )
                        LaunchedEffect(textValue) {
                            if (textValue.isNotBlank() || !currentStage.required) {
                                collectedData[currentStage.id] = textValue
                            } else {
                                collectedData.remove(currentStage.id)
                            }
                        }
                    }
                    
                    InputType.NUMBER -> {
                        var numberValue by remember(currentStageIndex) { 
                            mutableStateOf(currentStage.defaultValue?.toString() ?: "") 
                        }
                        OutlinedTextField(
                            value = numberValue,
                            onValueChange = { value ->
                                if (value.all { it.isDigit() || it == '.' }) {
                                    numberValue = value
                                }
                            },
                            label = { Text(currentStage.hint ?: "Enter number") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        LaunchedEffect(numberValue) {
                            numberValue.toDoubleOrNull()?.let {
                                collectedData[currentStage.id] = it
                            }
                        }
                    }
                    
                    InputType.DURATION -> {
                        var hours by remember(currentStageIndex) { mutableStateOf("") }
                        var minutes by remember(currentStageIndex) { mutableStateOf("") }
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = hours,
                                onValueChange = { if (it.all { char -> char.isDigit() }) hours = it },
                                label = { Text("Hours") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = minutes,
                                onValueChange = { if (it.all { char -> char.isDigit() }) minutes = it },
                                label = { Text("Minutes") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                        
                        LaunchedEffect(hours, minutes) {
                            val totalMinutes = (hours.toIntOrNull() ?: 0) * 60 + (minutes.toIntOrNull() ?: 0)
                            if (totalMinutes > 0) {
                                collectedData[currentStage.id] = totalMinutes
                            }
                        }
                    }
                    
                    InputType.CHOICE -> {
                        var selectedOption by remember(currentStageIndex) { 
                            mutableStateOf(currentStage.options?.firstOrNull()) 
                        }
                        
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            currentStage.options?.forEach { option ->
                                FilterChip(
                                    selected = selectedOption == option,
                                    onClick = { 
                                        selectedOption = option
                                        collectedData[currentStage.id] = option.value
                                    },
                                    label = { Text(option.label) },
                                    leadingIcon = {
                                        option.icon?.let { Text(it) }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                    
                    InputType.SCALE -> {
                        var scaleValue by remember(currentStageIndex) { 
                            mutableStateOf(currentStage.defaultValue as? Int ?: 3) 
                        }
                        
                        Column {
                            Slider(
                                value = scaleValue.toFloat(),
                                onValueChange = { 
                                    scaleValue = it.toInt()
                                    collectedData[currentStage.id] = scaleValue
                                },
                                valueRange = 1f..5f,
                                steps = 3,
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                currentStage.options?.forEach { option ->
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(option.icon ?: "")
                                        Text(
                                            option.label,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    else -> {
                        Text("Unsupported input type: ${currentStage.inputType}")
                    }
                }
            }
        },
        confirmButton = {
            val isLastStage = currentStageIndex == stages.size - 1
            val canProceed = !currentStage.required || collectedData.containsKey(currentStage.id)
            
            TextButton(
                onClick = {
                    if (isLastStage) {
                        onComplete(collectedData.toMap())
                    } else {
                        currentStageIndex++
                    }
                },
                enabled = canProceed
            ) {
                Text(if (isLastStage) "Save" else "Next")
            }
        },
        dismissButton = {
            Row {
                if (currentStageIndex > 0) {
                    TextButton(onClick = { currentStageIndex-- }) {
                        Text("Back")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
