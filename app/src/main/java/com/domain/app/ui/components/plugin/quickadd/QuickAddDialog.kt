// app/src/main/java/com/domain/app/ui/components/plugin/quickadd/QuickAddDialog.kt
package com.domain.app.ui.components.plugin.quickadd

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.domain.app.core.plugin.*
import com.domain.app.ui.components.core.sliders.HorizontalSlider
import com.domain.app.ui.components.core.sliders.VerticalSlider
import com.domain.app.ui.components.core.sliders.HorizontalSliderPresets

/**
 * Unified quick-add dialog that adapts to any plugin configuration.
 * This replaces all individual quick-add dialogs with a single, configurable component.
 * 
 * Usage:
 * ```kotlin
 * UnifiedQuickAddDialog(
 *     plugin = waterPlugin,
 *     onDismiss = { },
 *     onConfirm = { data -> saveData(data) }
 * )
 * ```
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedQuickAddDialog(
    plugin: Plugin,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, Any>) -> Unit,
    modifier: Modifier = Modifier
) {
    val config = plugin.getQuickAddConfig()
    
    if (config == null) {
        // Fallback to generic text input if no config
        GenericQuickAddContent(
            plugin = plugin,
            onDismiss = onDismiss,
            onConfirm = onConfirm
        )
        return
    }
    
    when (config.inputType) {
        InputType.SLIDER -> SliderQuickAddContent(
            plugin = plugin,
            config = config,
            onDismiss = onDismiss,
            onConfirm = onConfirm
        )
        InputType.CHOICE -> ChoiceQuickAddContent(
            plugin = plugin,
            config = config,
            onDismiss = onDismiss,
            onConfirm = onConfirm
        )
        InputType.SCALE -> ScaleQuickAddContent(
            plugin = plugin,
            config = config,
            onDismiss = onDismiss,
            onConfirm = onConfirm
        )
        InputType.MULTI_STAGE -> MultiStageQuickAddContent(
            plugin = plugin,
            stages = config.stages ?: emptyList(),
            onDismiss = onDismiss,
            onConfirm = onConfirm
        )
        else -> GenericQuickAddContent(
            plugin = plugin,
            onDismiss = onDismiss,
            onConfirm = onConfirm
        )
    }
}

/**
 * Slider-based quick add (e.g., Water intake)
 * Uses our new HorizontalSlider component
 */
@Composable
private fun SliderQuickAddContent(
    plugin: Plugin,
    config: QuickAddConfig,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, Any>) -> Unit
) {
    var value by remember { 
        mutableStateOf(config.defaultValue as? Float ?: 0f) 
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add ${plugin.metadata.name}",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = config.title,
                    style = MaterialTheme.typography.bodyLarge
                )
                
                // Use plugin-specific slider preset if available
                when (plugin.id) {
                    "water" -> {
                        HorizontalSliderPresets.waterIntakeSlider(
                            value = value,
                            onValueChange = { value = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    "mood" -> {
                        HorizontalSliderPresets.moodRatingSlider(
                            value = value,
                            onValueChange = { value = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    else -> {
                        // Generic horizontal slider
                        HorizontalSlider(
                            value = value,
                            onValueChange = { value = it },
                            valueRange = config.min?.toFloat() ?: 0f..config.max?.toFloat() ?: 100f,
                            steps = config.step?.toInt() ?: 0,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                // Optional hint text
                config.hint?.let { hint ->
                    Text(
                        text = hint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(mapOf(config.id to value))
                }
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

/**
 * Choice-based quick add (e.g., Activity type)
 */
@Composable
private fun ChoiceQuickAddContent(
    plugin: Plugin,
    config: QuickAddConfig,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, Any>) -> Unit
) {
    var selectedOption by remember { 
        mutableStateOf(config.options?.firstOrNull()) 
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add ${plugin.metadata.name}",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = config.title,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                config.options?.forEach { option ->
                    FilterChip(
                        selected = selectedOption == option,
                        onClick = { selectedOption = option },
                        label = { 
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                option.icon?.let { 
                                    Text(text = it, style = MaterialTheme.typography.titleMedium)
                                }
                                Text(text = option.label)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedOption?.let {
                        onConfirm(mapOf(config.id to it.value))
                    }
                },
                enabled = selectedOption != null
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

/**
 * Scale-based quick add with vertical slider
 */
@Composable
private fun ScaleQuickAddContent(
    plugin: Plugin,
    config: QuickAddConfig,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, Any>) -> Unit
) {
    var value by remember { 
        mutableStateOf(config.defaultValue as? Float ?: 3f) 
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add ${plugin.metadata.name}",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = config.title,
                    style = MaterialTheme.typography.bodyLarge
                )
                
                // Use vertical slider for scale inputs
                VerticalSlider(
                    value = value,
                    onValueChange = { value = it },
                    valueRange = config.min?.toFloat() ?: 1f..config.max?.toFloat() ?: 5f,
                    steps = ((config.max ?: 5) - (config.min ?: 1)).toInt(),
                    height = 200.dp,
                    showTicks = true,
                    labelFormatter = { 
                        when (plugin.id) {
                            "energy" -> {
                                when(it.roundToInt()) {
                                    1 -> "Exhausted"
                                    2 -> "Tired"
                                    3 -> "Normal"
                                    4 -> "Energetic"
                                    5 -> "Very Energetic"
                                    else -> it.roundToInt().toString()
                                }
                            }
                            else -> it.roundToInt().toString()
                        }
                    }
                )
                
                config.hint?.let { hint ->
                    Text(
                        text = hint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(mapOf(config.id to value))
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    )
}

/**
 * Multi-stage quick add for complex inputs
 */
@Composable
private fun MultiStageQuickAddContent(
    plugin: Plugin,
    stages: List<QuickAddStage>,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, Any>) -> Unit
) {
    var currentStageIndex by remember { mutableStateOf(0) }
    val collectedData = remember { mutableStateMapOf<String, Any>() }
    val currentStage = stages.getOrNull(currentStageIndex)
    
    if (currentStage == null) {
        LaunchedEffect(Unit) {
            onConfirm(collectedData.toMap())
        }
        return
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = plugin.metadata.name,
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Step ${currentStageIndex + 1} of ${stages.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            QuickAddStageContent(
                stage = currentStage,
                onValueChange = { value ->
                    if (value != null) {
                        collectedData[currentStage.id] = value
                    } else {
                        collectedData.remove(currentStage.id)
                    }
                }
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (currentStageIndex < stages.size - 1) {
                        currentStageIndex++
                    } else {
                        onConfirm(collectedData.toMap())
                    }
                },
                enabled = !currentStage.required || collectedData.containsKey(currentStage.id)
            ) {
                Text(if (currentStageIndex < stages.size - 1) "Next" else "Complete")
            }
        },
        dismissButton = {
            Row {
                if (currentStageIndex > 0) {
                    TextButton(
                        onClick = { currentStageIndex-- }
                    ) {
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

/**
 * Content for individual stage in multi-stage dialog
 */
@Composable
private fun QuickAddStageContent(
    stage: QuickAddStage,
    onValueChange: (Any?) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stage.title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        
        when (stage.inputType) {
            InputType.TEXT, InputType.NUMBER -> {
                var textValue by remember(stage.id) { 
                    mutableStateOf(stage.defaultValue?.toString() ?: "") 
                }
                
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { 
                        textValue = it
                        onValueChange(if (it.isNotBlank()) it else null)
                    },
                    label = { Text(stage.hint ?: "Enter value") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = stage.inputType == InputType.NUMBER,
                    isError = stage.required && textValue.isBlank()
                )
            }
            
            InputType.SLIDER -> {
                var sliderValue by remember(stage.id) { 
                    mutableStateOf(stage.defaultValue as? Float ?: 0f) 
                }
                
                HorizontalSlider(
                    value = sliderValue,
                    onValueChange = { 
                        sliderValue = it
                        onValueChange(it)
                    },
                    valueRange = stage.min?.toFloat() ?: 0f..stage.max?.toFloat() ?: 100f,
                    steps = stage.step?.toInt() ?: 0,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            InputType.CHOICE -> {
                var selectedOption by remember(stage.id) { 
                    mutableStateOf(stage.options?.firstOrNull()) 
                }
                
                LaunchedEffect(selectedOption) {
                    onValueChange(selectedOption?.value)
                }
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    stage.options?.forEach { option ->
                        FilterChip(
                            selected = selectedOption == option,
                            onClick = { selectedOption = option },
                            label = { Text(option.label) },
                            leadingIcon = {
                                option.icon?.let { Text(it) }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            
            else -> {
                Text("Unsupported input type: ${stage.inputType}")
            }
        }
        
        stage.hint?.let { hint ->
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Generic fallback for plugins without specific configuration
 */
@Composable
private fun GenericQuickAddContent(
    plugin: Plugin,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, Any>) -> Unit
) {
    var inputValue by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Add ${plugin.metadata.name}")
        },
        text = {
            OutlinedTextField(
                value = inputValue,
                onValueChange = { inputValue = it },
                label = { Text("Value") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(mapOf("value" to inputValue))
                },
                enabled = inputValue.isNotBlank()
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
