package com.domain.app.ui.components.plugin.quickadd

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.domain.app.core.plugin.*
import com.domain.app.ui.components.core.sliders.HorizontalSlider
import com.domain.app.ui.components.core.sliders.VerticalSlider
import com.domain.app.ui.components.core.carousel.Carousel
import com.domain.app.ui.components.core.carousel.CarouselOption
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
    val steps = if (config.step != null) {
        val stepSize = (config.step as Number).toFloat()
        ((range.endInclusive - range.start) / stepSize).toInt() - 1
    } else 0
    
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                HorizontalSlider(
                    value = value,
                    onValueChange = { value = it },
                    valueRange = range,
                    steps = steps,
                    showLabel = true,
                    labelFormatter = { v -> 
                        "${v.roundToInt()}${config.unit?.let { " $it" } ?: ""}"
                    },
                    modifier = Modifier.fillMaxWidth()
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
        mutableStateOf((config.defaultValue as? Number)?.toFloat() ?: 3f)
    }
    
    val range = ((config.min as? Number)?.toFloat() ?: 1f)..((config.max as? Number)?.toFloat() ?: 5f)
    val steps = if (config.step != null) {
        val stepSize = (config.step as Number).toFloat()
        ((range.endInclusive - range.start) / stepSize).toInt() - 1
    } else (range.endInclusive.toInt() - range.start.toInt() - 1)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(config.title)
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                VerticalSlider(
                    value = value,
                    onValueChange = { value = it },
                    valueRange = range,
                    steps = steps,
                    height = 200.dp,
                    showLabel = !config.showValue,
                    labelFormatter = { v -> 
                        when {
                            config.topLabel != null && v >= range.endInclusive - 0.5f -> config.topLabel!!
                            config.bottomLabel != null && v <= range.start + 0.5f -> config.bottomLabel!!
                            else -> v.roundToInt().toString()
                        }
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
 * Choice-based quick add
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
 * Time range quick add
 */
@Composable
private fun TimeRangeQuickAddContent(
    plugin: Plugin,
    config: QuickAddConfig,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, Any>) -> Unit
) {
    var startTime by remember { mutableStateOf(23f) }  // 11 PM
    var endTime by remember { mutableStateOf(7f) }     // 7 AM
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(config.title)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Bedtime: ${startTime.roundToInt()}:00")
                HorizontalSlider(
                    value = startTime,
                    onValueChange = { startTime = it },
                    valueRange = 0f..24f,
                    steps = 23,
                    showLabel = false
                )
                
                Text("Wake time: ${endTime.roundToInt()}:00")
                HorizontalSlider(
                    value = endTime,
                    onValueChange = { endTime = it },
                    valueRange = 0f..24f,
                    steps = 23,
                    showLabel = false
                )
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
 * Multi-stage quick add
 */
@Composable
private fun MultiStageQuickAddContent(
    plugin: Plugin,
    config: QuickAddConfig,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, Any>) -> Unit
) {
    var currentStage by remember { mutableStateOf(0) }
    val stages = config.stages ?: return
    val values = remember { mutableStateMapOf<String, Any>() }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stages[currentStage].title)
        },
        text = {
            StageInput(
                stage = stages[currentStage],
                onValueChange = { value ->
                    if (value != null) {
                        values[stages[currentStage].id] = value
                    }
                }
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (currentStage < stages.size - 1) {
                        currentStage++
                    } else {
                        onConfirm(values.toMap())
                    }
                }
            ) {
                Text(if (currentStage < stages.size - 1) "Next" else "Add")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                if (currentStage > 0) {
                    currentStage--
                } else {
                    onDismiss()
                }
            }) {
                Text(if (currentStage > 0) "Back" else "Cancel")
            }
        }
    )
}

/**
 * Composite quick add (multiple inputs on one screen)
 */
@Composable
private fun CompositeQuickAddContent(
    plugin: Plugin,
    config: QuickAddConfig,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, Any>) -> Unit
) {
    val inputs = config.inputs ?: return
    val values = remember { mutableStateMapOf<String, Any>() }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(config.title)
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(inputs) { input ->
                    CompositeInputField(
                        input = input,
                        onValueChange = { value ->
                            if (value != null) {
                                values[input.id] = value
                            }
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (values.size == inputs.size) {
                        onConfirm(values.toMap())
                    }
                },
                enabled = values.size == inputs.size
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
 * Individual stage input renderer
 * Fixed: Using inputType instead of type
 */
@Composable
private fun StageInput(
    stage: QuickAddStage,
    onValueChange: (Any?) -> Unit
) {
    when (stage.inputType) {  // FIX: Changed from stage.type to stage.inputType
        InputType.HORIZONTAL_SLIDER -> {
            var sliderValue by remember { 
                mutableStateOf(stage.defaultValue as? Float ?: 0f)
            }
            
            LaunchedEffect(sliderValue) {
                onValueChange(sliderValue)
            }
            
            val range = ((stage.min as? Number)?.toFloat() ?: 0f)..((stage.max as? Number)?.toFloat() ?: 100f)
            val steps = if (stage.step != null) {
                val stepSize = (stage.step as Number).toFloat()
                ((range.endInclusive - range.start) / stepSize).toInt() - 1
            } else 0
            
            HorizontalSlider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                valueRange = range,
                steps = steps,
                showLabel = true,
                labelFormatter = { v ->
                    "${v.roundToInt()}${stage.unit?.let { " $it" } ?: ""}"
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        InputType.CHOICE -> {
            var selectedOption by remember { 
                mutableStateOf(stage.options?.firstOrNull())
            }
            
            LaunchedEffect(selectedOption) {
                selectedOption?.let { onValueChange(it.value) }
            }
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                stage.options?.forEach { option ->
                    FilterChip(
                        selected = selectedOption == option,
                        onClick = { selectedOption = option },
                        label = { Text(option.label) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        
        else -> {
            // Default to text input
            var textValue by remember { 
                mutableStateOf(stage.defaultValue?.toString() ?: "")
            }
            
            LaunchedEffect(textValue) {
                if (textValue.isNotBlank()) {
                    onValueChange(textValue)
                }
            }
            
            OutlinedTextField(
                value = textValue,
                onValueChange = { textValue = it },
                label = { Text(stage.placeholder ?: "Enter value") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Composite input field renderer
 * Fixed: QuickAddInput doesn't have step property
 */
@Composable
private fun CompositeInputField(
    input: QuickAddInput,
    onValueChange: (Any?) -> Unit
) {
    Column {
        Text(
            text = input.label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        when (input.type) {
            InputType.HORIZONTAL_SLIDER -> {
                var sliderValue by remember { 
                    mutableStateOf((input.defaultValue as? Number)?.toFloat() ?: 0f)
                }
                
                LaunchedEffect(sliderValue) {
                    onValueChange(sliderValue)
                }
                
                val range = ((input.min as? Number)?.toFloat() ?: 0f)..((input.max as? Number)?.toFloat() ?: 100f)
                // FIX: QuickAddInput doesn't have step, so we calculate steps differently
                val steps = 0  // No step property in QuickAddInput, use continuous slider
                
                HorizontalSlider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = range,
                    steps = steps,
                    showLabel = true,
                    labelFormatter = { v ->
                        "${v.roundToInt()}${input.unit?.let { " $it" } ?: ""}"
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            InputType.CAROUSEL -> {
                var selectedOption by remember { 
                    mutableStateOf(
                        input.options?.find { it.value == input.defaultValue }
                            ?: input.options?.firstOrNull()
                    )
                }
                
                LaunchedEffect(selectedOption) {
                    selectedOption?.let { onValueChange(it.value) }
                }
                
                // FIX: Create proper CarouselOption from QuickOption
                val carouselOptions = input.options?.map { 
                    CarouselOption(it.label, it.value, it.icon)
                } ?: emptyList()
                
                Carousel(
                    options = carouselOptions,
                    selectedOption = carouselOptions.find { 
                        it.value == selectedOption?.value 
                    },
                    onOptionSelected = { selected ->
                        input.options?.find { it.value == selected.value }?.let {
                            selectedOption = it
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            else -> {
                // Default to text input
                var textValue by remember { 
                    mutableStateOf(input.defaultValue?.toString() ?: "")
                }
                
                LaunchedEffect(textValue) {
                    if (textValue.isNotBlank()) {
                        onValueChange(textValue)
                    }
                }
                
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
