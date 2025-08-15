package com.domain.app.ui.components.plugin.quickadd

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.domain.app.core.plugin.*
import com.domain.app.core.validation.ValidationResult
import com.domain.app.ui.components.core.input.ValidatedTextField
import com.domain.app.ui.components.core.sliders.HorizontalSlider
import com.domain.app.ui.components.core.sliders.VerticalSlider
import com.domain.app.ui.components.core.sliders.RangeSlider
import com.domain.app.ui.components.core.carousel.Carousel
import com.domain.app.ui.components.core.carousel.CarouselOption
import com.domain.app.ui.components.core.button.TileButtonGrid
import kotlin.math.roundToInt

/**
 * Quick add dialog for plugins
 * Renders appropriate input components based on plugin configuration
 */
@Composable
fun QuickAddDialog(
    plugin: Plugin,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, Any>) -> Unit
) {
    val config = plugin.getQuickAddConfig() ?: return
    
    when {
        // Multi-stage input
        config.stages != null -> {
            MultiStageQuickAddContent(
                plugin = plugin,
                config = config,
                onDismiss = onDismiss,
                onConfirm = onConfirm
            )
        }
        
        // Composite inputs (multiple inputs on one screen)
        config.inputs != null -> {
            CompositeQuickAddContent(
                plugin = plugin,
                config = config,
                onDismiss = onDismiss,
                onConfirm = onConfirm
            )
        }
        
        // Single input based on type
        else -> {
            when (config.inputType) {
                InputType.HORIZONTAL_SLIDER -> {
                    HorizontalSliderQuickAddContent(
                        plugin = plugin,
                        config = config,
                        onDismiss = onDismiss,
                        onConfirm = onConfirm
                    )
                }
                InputType.VERTICAL_SLIDER -> {
                    VerticalSliderQuickAddContent(
                        plugin = plugin,
                        config = config,
                        onDismiss = onDismiss,
                        onConfirm = onConfirm
                    )
                }
                InputType.CAROUSEL -> {
                    CarouselQuickAddContent(
                        plugin = plugin,
                        config = config,
                        onDismiss = onDismiss,
                        onConfirm = onConfirm
                    )
                }
                InputType.CHOICE -> {
                    ChoiceQuickAddContent(
                        plugin = plugin,
                        config = config,
                        onDismiss = onDismiss,
                        onConfirm = onConfirm
                    )
                }
                InputType.NUMBER -> {
                    NumberQuickAddContent(
                        plugin = plugin,
                        config = config,
                        onDismiss = onDismiss,
                        onConfirm = onConfirm
                    )
                }
                InputType.TEXT -> {
                    TextQuickAddContent(
                        plugin = plugin,
                        config = config,
                        onDismiss = onDismiss,
                        onConfirm = onConfirm
                    )
                }
                InputType.BOOLEAN -> {
                    BooleanQuickAddContent(
                        plugin = plugin,
                        config = config,
                        onDismiss = onDismiss,
                        onConfirm = onConfirm
                    )
                }
                InputType.TIME_RANGE -> {
                    TimeRangeQuickAddContent(
                        plugin = plugin,
                        config = config,
                        onDismiss = onDismiss,
                        onConfirm = onConfirm
                    )
                }
                else -> {
                    // Fallback to simple number input
                    NumberQuickAddContent(
                        plugin = plugin,
                        config = config,
                        onDismiss = onDismiss,
                        onConfirm = onConfirm
                    )
                }
            }
        }
    }
}

/**
 * Horizontal slider quick add
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
    
    val range = ((config.min as? Number)?.toFloat() ?: 0f)..((config.max as? Number)?.toFloat() ?: 100f)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(config.title)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                HorizontalSlider(
                    value = value,
                    onValueChange = { value = it },
                    valueRange = range,
                    labelFormatter = { v ->
                        "${v.roundToInt()}${config.unit?.let { " $it" } ?: ""}"
                    },
                    modifier = Modifier
                )
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
 * Vertical slider quick add
 */
@Composable
private fun VerticalSliderQuickAddContent(
    plugin: Plugin,
    config: QuickAddConfig,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, Any>) -> Unit
) {
    var value by remember { 
        mutableStateOf((config.defaultValue as? Number)?.toFloat() ?: 0f)
    }
    
    val range = ((config.min as? Number)?.toFloat() ?: 0f)..((config.max as? Number)?.toFloat() ?: 10f)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(config.title)
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.height(250.dp)
            ) {
                VerticalSlider(
                    value = value,
                    onValueChange = { value = it },
                    valueRange = range,
                    showLabel = config.showValue,
                    labelFormatter = { v ->
                        when {
                            config.topLabel != null && v >= range.endInclusive - 0.5f -> config.topLabel!!
                            config.bottomLabel != null && v <= range.start + 0.5f -> config.bottomLabel!!
                            else -> v.roundToInt().toString()
                        }
                    },
                    height = 200.dp,
                    modifier = Modifier
                )
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
 * Carousel-based quick add
 */
@Composable
private fun CarouselQuickAddContent(
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
            config.options?.let { options ->
                Carousel(
                    options = options.map { option ->
                        CarouselOption(
                            label = option.label,
                            icon = option.icon,
                            value = option.value
                        )
                    },
                    selectedOption = selectedOption?.let { selected ->
                        CarouselOption(
                            label = selected.label,
                            icon = selected.icon,
                            value = selected.value
                        )
                    },
                    onOptionSelected = { carouselOption ->
                        selectedOption = options.find { it.value == carouselOption.value }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
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
 * Choice-based quick add using tile buttons
 */
@Composable
private fun ChoiceQuickAddContent(
    plugin: Plugin,
    config: QuickAddConfig,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, Any>) -> Unit
) {
    var selectedOption by remember { mutableStateOf<String?>(null) }
    val options = config.options ?: emptyList()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(config.title)
        },
        text = {
            TileButtonGrid(
                options = options.map { it.label },
                selectedOption = selectedOption,
                onOptionSelected = { selectedOption = it },
                columns = 3
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedOption?.let { selected ->
                        val option = options.find { it.label == selected }
                        option?.let {
                            onConfirm(mapOf(config.id to it.value))
                        }
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
 * Number input quick add
 */
@Composable
private fun NumberQuickAddContent(
    plugin: Plugin,
    config: QuickAddConfig,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, Any>) -> Unit
) {
    var value by remember { 
        mutableStateOf(config.defaultValue?.toString() ?: "")
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(config.title)
        },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(config.placeholder ?: "Value") },
                suffix = { config.unit?.let { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    value.toFloatOrNull()?.let {
                        onConfirm(mapOf(config.id to it))
                    }
                },
                enabled = value.toFloatOrNull() != null
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
 * Text input quick add
 */
@Composable
private fun TextQuickAddContent(
    plugin: Plugin,
    config: QuickAddConfig,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, Any>) -> Unit
) {
    var value by remember { 
        mutableStateOf(config.defaultValue?.toString() ?: "")
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(config.title)
        },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(config.placeholder ?: "Enter text") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (value.isNotBlank()) {
                        onConfirm(mapOf(config.id to value))
                    }
                },
                enabled = value.isNotBlank()
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
 * Boolean toggle quick add
 */
@Composable
private fun BooleanQuickAddContent(
    plugin: Plugin,
    config: QuickAddConfig,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, Any>) -> Unit
) {
    var checked by remember { 
        mutableStateOf(config.defaultValue as? Boolean ?: false)
    }
    
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
 * Time range quick add using RangeSlider
 * Used for Sleep plugin to select bedtime and wake time
 */
@Composable
private fun TimeRangeQuickAddContent(
    plugin: Plugin,
    config: QuickAddConfig,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, Any>) -> Unit
) {
    // Initialize from config defaults or use standard sleep times
    var startTime by remember { 
        mutableStateOf(
            (config.defaultValue as? Map<*, *>)?.get("bedtime") as? Float ?: 23f
        )
    }
    var endTime by remember { 
        mutableStateOf(
            (config.defaultValue as? Map<*, *>)?.get("waketime") as? Float ?: 7f
        )
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(config.title)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Select sleep time",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                // RangeSlider for time selection
                RangeSlider(
                    value = startTime..endTime,
                    onValueChange = { range ->
                        startTime = range.start
                        endTime = range.endInclusive
                    },
                    valueRange = 0f..48f,  // 48 hours to handle crossing midnight
                    steps = 47,  // 1-hour increments
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Time labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("12AM", style = MaterialTheme.typography.labelSmall)
                    Text("6AM", style = MaterialTheme.typography.labelSmall)
                    Text("12PM", style = MaterialTheme.typography.labelSmall)
                    Text("6PM", style = MaterialTheme.typography.labelSmall)
                    Text("12AM", style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(mapOf(
                        "bedtime" to startTime,
                        "waketime" to endTime
                    ))
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
 * Multi-stage quick add implementation
 */
@Composable
private fun MultiStageQuickAddContent(
    plugin: Plugin,
    config: QuickAddConfig,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, Any>) -> Unit
) {
    val stages = config.stages ?: return
    var currentStageIndex by remember { mutableStateOf(0) }
    val results = remember { mutableStateMapOf<String, Any>() }
    
    if (stages.isEmpty()) {
        // Fallback if no stages defined
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text("Configuration Error")
            },
            text = {
                Text("No stages defined for multi-stage input")
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("OK")
                }
            }
        )
        return
    }
    
    val currentStage = stages[currentStageIndex]
    var stageValue by remember(currentStageIndex) { 
        mutableStateOf(results[currentStage.id] ?: currentStage.defaultValue ?: "")
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(currentStage.title)
                // Progress indicator
                LinearProgressIndicator(
                    progress = (currentStageIndex + 1f) / stages.size,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Stage-specific input based on type
                when (currentStage.inputType) {
                    InputType.TEXT -> {
                        OutlinedTextField(
                            value = stageValue.toString(),
                            onValueChange = { stageValue = it },
                            label = { Text(currentStage.placeholder ?: "Enter value") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    InputType.NUMBER -> {
                        OutlinedTextField(
                            value = stageValue.toString(),
                            onValueChange = { 
                                stageValue = it.toFloatOrNull() ?: 0f
                            },
                            label = { Text(currentStage.placeholder ?: "Enter number") },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    InputType.CHOICE -> {
                        Column {
                            currentStage.options?.forEach { option ->
                                FilterChip(
                                    selected = stageValue == option.value,
                                    onClick = { stageValue = option.value },
                                    label = { Text(option.label) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                    else -> {
                        Text("Input type ${currentStage.inputType} not yet implemented")
                    }
                }
            }
        },
        confirmButton = {
            Row {
                if (currentStageIndex > 0) {
                    TextButton(
                        onClick = {
                            results[currentStage.id] = stageValue
                            currentStageIndex--
                        }
                    ) {
                        Text("Back")
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                TextButton(
                    onClick = {
                        results[currentStage.id] = stageValue
                        if (currentStageIndex < stages.size - 1) {
                            currentStageIndex++
                        } else {
                            onConfirm(results.toMap())
                        }
                    },
                    enabled = when {
                        currentStage.required && stageValue.toString().isBlank() -> false
                        else -> true
                    }
                ) {
                    Text(
                        if (currentStageIndex < stages.size - 1) "Next" else "Complete"
                    )
                }
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
 * Composite quick add with multiple inputs on one screen
 * THIS IS THE KEY IMPLEMENTATION FOR MOVEMENT PLUGIN
 */
@Composable
private fun CompositeQuickAddContent(
    plugin: Plugin,
    config: QuickAddConfig,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, Any>) -> Unit
) {
    val inputs = config.inputs ?: return
    val results = remember { mutableStateMapOf<String, Any>() }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(config.title)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                inputs.forEach { input ->
                    when (input.type) {
                        InputType.HORIZONTAL_SLIDER -> {
                            var value by remember { 
                                mutableStateOf((input.defaultValue as? Number)?.toFloat() ?: 0f)
                            }
                            Column {
                                Text(
                                    text = input.label,
                                    style = MaterialTheme.typography.labelMedium
                                )
                                HorizontalSlider(
                                    value = value,
                                    onValueChange = { 
                                        value = it
                                        results[input.id] = it
                                    },
                                    valueRange = ((input.min as? Number)?.toFloat() ?: 0f)..
                                            ((input.max as? Number)?.toFloat() ?: 100f),
                                    showLabel = true,
                                    labelFormatter = { v ->
                                        "${v.roundToInt()}${input.unit?.let { " $it" } ?: ""}"
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        InputType.TEXT -> {
                            var value by remember { 
                                mutableStateOf((input.defaultValue as? String) ?: "")
                            }
                            OutlinedTextField(
                                value = value,
                                onValueChange = { 
                                    value = it
                                    results[input.id] = it
                                },
                                label = { Text(input.label) },
                                placeholder = { Text(input.placeholder ?: "") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        else -> {
                            Text("Input type ${input.type} not implemented for composite")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // Validate required fields
                    val allRequiredPresent = inputs
                        .filter { it.required }
                        .all { results.containsKey(it.id) }
                    
                    if (allRequiredPresent) {
                        onConfirm(results.toMap())
                    }
                },
                enabled = inputs.filter { it.required }.all { results.containsKey(it.id) }
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
