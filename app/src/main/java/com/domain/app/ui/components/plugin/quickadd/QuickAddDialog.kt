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
import com.domain.app.ui.components.core.sliders.HorizontalSliderDefaults
import com.domain.app.ui.components.core.sliders.VerticalSlider
import com.domain.app.ui.components.core.sliders.VerticalSliderDefaults
import com.domain.app.ui.components.core.sliders.SliderDescriptions
import kotlin.math.roundToInt

/**
 * Unified quick-add dialog that adapts to any plugin configuration.
 * This replaces all individual quick-add dialogs with a single, configurable component.
 */
@Composable
fun UnifiedQuickAddDialog(
    plugin: Plugin,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, Any>) -> Unit
) {
    val config = plugin.getQuickAddConfig()
    
    when {
        plugin.metadata.supportsMultiStage -> {
            config?.stages?.let { stages ->
                MultiStageQuickAddContent(
                    plugin = plugin,
                    stages = stages,
                    onDismiss = onDismiss,
                    onConfirm = onConfirm
                )
            } ?: GenericQuickAddContent(plugin, onDismiss, onConfirm)
        }
        
        config != null -> {
            when (config.inputType) {
                InputType.TEXT -> TextQuickAddContent(plugin, config, onDismiss, onConfirm)
                InputType.NUMBER -> NumberQuickAddContent(plugin, config, onDismiss, onConfirm)
                InputType.SLIDER -> SliderQuickAddContent(plugin, config, onDismiss, onConfirm) // Backward compatibility
                InputType.VERTICAL_SLIDER -> VerticalSliderQuickAddContent(plugin, config, onDismiss, onConfirm)
                InputType.HORIZONTAL_SLIDER -> HorizontalSliderQuickAddContent(plugin, config, onDismiss, onConfirm)
                InputType.CHOICE -> ChoiceQuickAddContent(plugin, config, onDismiss, onConfirm)
                InputType.BOOLEAN -> BooleanQuickAddContent(plugin, config, onDismiss, onConfirm)
                InputType.DATE -> DateQuickAddContent(plugin, config, onDismiss, onConfirm)
                InputType.TIME -> TimeQuickAddContent(plugin, config, onDismiss, onConfirm)
                else -> GenericQuickAddContent(plugin, onDismiss, onConfirm)
            }
        }
        
        else -> GenericQuickAddContent(plugin, onDismiss, onConfirm)
    }
}

/**
 * Text input quick add
 */
@Composable
private fun TextQuickAddContent(
    plugin: Plugin,
    config: QuickAddConfig,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, Any>) -> Unit
) {
    var text by remember { mutableStateOf(config.defaultValue as? String ?: "") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(config.title)
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(config.placeholder ?: "Enter value") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 3
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(mapOf(config.id to text))
                },
                enabled = text.isNotBlank()
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
 * Number input quick add
 */
@Composable
private fun NumberQuickAddContent(
    plugin: Plugin,
    config: QuickAddConfig,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, Any>) -> Unit
) {
    var number by remember { mutableStateOf(config.defaultValue?.toString() ?: "") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(config.title)
        },
        text = {
            OutlinedTextField(
                value = number,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() || newValue.toDoubleOrNull() != null) {
                        number = newValue
                    }
                },
                label = { Text(config.placeholder ?: "Enter number") },
                suffix = { config.unit?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    number.toDoubleOrNull()?.let {
                        onConfirm(mapOf(config.id to it))
                    }
                },
                enabled = number.toDoubleOrNull() != null
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
 * Vertical slider quick add (for ratings, mood, satisfaction, etc.)
 */
@Composable
private fun VerticalSliderQuickAddContent(
    plugin: Plugin,
    config: QuickAddConfig,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, Any>) -> Unit
) {
    var value by remember { 
        mutableStateOf((config.defaultValue as? Number)?.toFloat() ?: 3f)
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(config.title)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Optional emoji/icon display based on value
                config.options?.find { (it.value as? Number)?.toInt() == value.roundToInt() }?.let { option ->
                    option.icon?.let { icon ->
                        Text(
                            text = icon,
                            style = MaterialTheme.typography.displaySmall
                        )
                    }
                }
                
                // Vertical slider
                VerticalSlider(
                    value = value,
                    onValueChange = { value = it },
                    valueRange = ((config.min as? Number)?.toFloat() ?: 1f)..((config.max as? Number)?.toFloat() ?: 5f),
                    steps = ((config.max as? Number)?.toFloat()?.minus((config.min as? Number)?.toFloat() ?: 1f))?.toInt()?.minus(1) ?: 3,
                    height = 200.dp,
                    showLabel = true,
                    showTicks = true,
                    colors = when(plugin.id) {
                        "mood" -> VerticalSliderDefaults.moodColors()
                        "energy" -> VerticalSliderDefaults.energyColors()
                        "stress" -> VerticalSliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.error,
                            activeTrackColor = MaterialTheme.colorScheme.error
                        )
                        "sleep" -> VerticalSliderDefaults.sleepColors()
                        else -> VerticalSliderDefaults.colors()
                    },
                    labelFormatter = when(plugin.id) {
                        "mood" -> SliderDescriptions.mood
                        "energy" -> SliderDescriptions.energy
                        "stress" -> SliderDescriptions.stress
                        "sleep" -> SliderDescriptions.sleep
                        else -> { v -> 
                            config.options?.find { (it.value as? Number)?.toFloat() == v }?.label 
                                ?: v.roundToInt().toString()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Optional description text
                config.options?.find { (it.value as? Number)?.toInt() == value.roundToInt() }?.let { option ->
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(mapOf(config.id to value.roundToInt()))
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
 * Horizontal slider quick add (for quantities, amounts, durations, etc.)
 */
@Composable
private fun HorizontalSliderQuickAddContent(
    plugin: Plugin,
    config: QuickAddConfig,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, Any>) -> Unit
) {
    var value by remember { 
        mutableStateOf((config.defaultValue as? Number)?.toFloat() ?: 0f)
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(config.title)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Use horizontal slider for quantities
                HorizontalSlider(
                    value = value,
                    onValueChange = { value = it },
                    valueRange = ((config.min as? Number)?.toFloat() ?: 0f)..((config.max as? Number)?.toFloat() ?: 100f),
                    steps = if (config.step != null) {
                        ((config.max as? Number)?.toFloat()?.minus((config.min as? Number)?.toFloat() ?: 0f))?.div((config.step as Number).toFloat())?.toInt() ?: 0
                    } else 0,
                    showLabel = true,
                    showTicks = config.step != null,
                    labelFormatter = { v -> 
                        "${v.roundToInt()}${config.unit?.let { " $it" } ?: ""}"
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Preset buttons if available
                config.presets?.let { presets ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        presets.forEach { preset ->
                            FilterChip(
                                selected = value == (preset.value as? Number)?.toFloat(),
                                onClick = { value = (preset.value as? Number)?.toFloat() ?: 0f },
                                label = { Text(preset.label) }
                            )
                        }
                    }
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
 * Generic slider quick add (backward compatibility for plugins still using InputType.SLIDER)
 * Defaults to horizontal slider behavior
 */
@Composable
private fun SliderQuickAddContent(
    plugin: Plugin,
    config: QuickAddConfig,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, Any>) -> Unit
) {
    // For backward compatibility, use horizontal slider as default
    HorizontalSliderQuickAddContent(plugin, config, onDismiss, onConfirm)
}

/**
 * Choice-based quick add (e.g., selecting from options)
 */
@Composable
private fun ChoiceQuickAddContent(
    plugin: Plugin,
    config: QuickAddConfig,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, Any>) -> Unit
) {
    var selectedOption by remember { mutableStateOf(config.options?.firstOrNull()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(config.title)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                config.options?.forEach { option ->
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
 * Boolean quick add (toggle/checkbox)
 */
@Composable
private fun BooleanQuickAddContent(
    plugin: Plugin,
    config: QuickAddConfig,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, Any>) -> Unit
) {
    var checked by remember { mutableStateOf(config.defaultValue as? Boolean ?: false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(config.title)
        },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(config.placeholder ?: "Enable")
                Switch(
                    checked = checked,
                    onCheckedChange = { checked = it }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(mapOf(config.id to checked))
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
 * Date quick add
 */
@Composable
private fun DateQuickAddContent(
    plugin: Plugin,
    config: QuickAddConfig,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, Any>) -> Unit
) {
    // TODO: Implement date picker
    // For now, fallback to generic
    GenericQuickAddContent(plugin, onDismiss, onConfirm)
}

/**
 * Time quick add
 */
@Composable
private fun TimeQuickAddContent(
    plugin: Plugin,
    config: QuickAddConfig,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, Any>) -> Unit
) {
    // TODO: Implement time picker
    // For now, fallback to generic
    GenericQuickAddContent(plugin, onDismiss, onConfirm)
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        stage.title?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        stage.description?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        when (stage.inputType) {
            InputType.TEXT -> {
                var text by remember(stage.id) { 
                    mutableStateOf(stage.defaultValue as? String ?: "") 
                }
                
                LaunchedEffect(text) {
                    onValueChange(text.ifEmpty { null })
                }
                
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stage.placeholder ?: "Enter value") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            InputType.NUMBER -> {
                var number by remember(stage.id) { 
                    mutableStateOf(stage.defaultValue?.toString() ?: "") 
                }
                
                LaunchedEffect(number) {
                    onValueChange(number.toDoubleOrNull())
                }
                
                OutlinedTextField(
                    value = number,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.toDoubleOrNull() != null) {
                            number = newValue
                        }
                    },
                    label = { Text(stage.placeholder ?: "Enter number") },
                    suffix = { stage.unit?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            InputType.SLIDER, InputType.HORIZONTAL_SLIDER -> {
                var sliderValue by remember(stage.id) { 
                    mutableStateOf(stage.defaultValue as? Float ?: 50f) 
                }
                
                val range = when {
                    stage.min != null && stage.max != null -> {
                        (stage.min as Number).toFloat()..(stage.max as Number).toFloat()
                    }
                    else -> 0f..100f
                }
                
                val steps = (stage.step as? Number)?.toInt() ?: 0
                
                HorizontalSlider(
                    value = sliderValue,
                    onValueChange = { 
                        sliderValue = it
                        onValueChange(it)
                    },
                    valueRange = range,
                    steps = steps,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text(
                    text = "${sliderValue.roundToInt()}${stage.unit?.let { " $it" } ?: ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            InputType.VERTICAL_SLIDER -> {
                var sliderValue by remember(stage.id) { 
                    mutableStateOf(stage.defaultValue as? Float ?: 3f) 
                }
                
                val range = when {
                    stage.min != null && stage.max != null -> {
                        (stage.min as Number).toFloat()..(stage.max as Number).toFloat()
                    }
                    else -> 1f..5f
                }
                
                VerticalSlider(
                    value = sliderValue,
                    onValueChange = { 
                        sliderValue = it
                        onValueChange(it)
                    },
                    valueRange = range,
                    steps = range.endInclusive.toInt() - range.start.toInt() - 1,
                    height = 150.dp,
                    showLabel = true,
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
