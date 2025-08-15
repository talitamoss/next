package com.domain.app.ui.components.plugin.quickadd

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.domain.app.core.plugin.*
import com.domain.app.ui.components.core.sliders.HorizontalSlider
import com.domain.app.ui.components.core.sliders.VerticalSlider
import com.domain.app.ui.components.core.sliders.RangeSlider
import com.domain.app.ui.components.core.sliders.RangeSliderDefaults
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
            Column {
                Slider(
                    value = value,
                    onValueChange = { value = it },
                    valueRange = range,
                    steps = ((config.step as? Number)?.toInt() ?: 0) - 1,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text(
                    text = "${value.roundToInt()} ${config.unit ?: ""}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
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
 * Vertical slider quick add (using rotated slider or custom implementation)
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
    
    val range = ((config.min as? Number)?.toFloat() ?: 0f)..((config.max as? Number)?.toFloat() ?: 100f)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(config.title)
        },
        text = {
            Column(
                modifier = Modifier.height(200.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                VerticalSlider(
                    value = value,
                    onValueChange = { value = it },
                    valueRange = range,
                    enabled = true,
                    showLabel = config.showValue,
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
<<<<<<< Updated upstream
            Text(config.title)
=======
<<<<<<< Updated upstream
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
>>>>>>> Stashed changes
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // TODO: Implement RangeSlider UI
                Text("Time range selection not yet implemented")
                Text("Bedtime: ${startTime.toInt()}:00")
                Text("Wake time: ${endTime.toInt()}:00")
            }
        },
=======
            Text(config.title)
        },
	text = {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Show current selection
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Bedtime: ${startTime.toInt()}:00")
                Text("Wake: ${endTime.toInt()}:00")
            }
        }
        
        // Add the RangeSlider
        RangeSlider(
            startValue = startTime,
            endValue = if (endTime < startTime) endTime + 24f else endTime,
            onRangeChange = { start, end ->
                startTime = start % 24
                endTime = end % 24
            },
            valueRange = 0f..48f,
            steps = 47,
            minRange = 1f,
            showLabels = false,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Time markers
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

>>>>>>> Stashed changes
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
    
    // TODO: Implement multi-stage UI
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Multi-stage not implemented")
        },
        text = {
            Text("This plugin uses multi-stage input which is not yet implemented")
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
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
    
    // Initialize default values
    LaunchedEffect(inputs) {
        inputs.forEach { input ->
            input.defaultValue?.let { 
                results[input.id] = it 
            }
        }
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
                inputs.forEach { input ->
                    when (input.type) {
                        InputType.CAROUSEL -> {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = input.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                input.options?.let { options ->
                                    val selectedOption = results[input.id] as? String
                                    
                                    Carousel(
                                        options = options.map { option ->
                                            CarouselOption(
                                                label = option.label,
                                                value = option.value,
                                                icon = option.icon
                                            )
                                        },
                                        selectedOption = options.find { it.value == selectedOption }?.let { selected ->
                                            CarouselOption(
                                                label = selected.label,
                                                value = selected.value,
                                                icon = selected.icon
                                            )
                                        },
                                        onOptionSelected = { carouselOption ->
                                            results[input.id] = carouselOption.value
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        height = 60.dp
                                    )
                                }
                            }
                        }
                        
                        InputType.HORIZONTAL_SLIDER -> {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = input.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    
                                    val value = (results[input.id] as? Number)?.toFloat() ?: 0f
                                    Text(
                                        text = if (input.unit != null) {
                                            "${value.roundToInt()} ${input.unit}"
                                        } else {
                                            formatSliderValue(value, input)
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                val currentValue = (results[input.id] as? Number)?.toFloat() 
                                    ?: (input.defaultValue as? Number)?.toFloat() 
                                    ?: 0f
                                    
                                val range = ((input.min as? Number)?.toFloat() ?: 0f)..
                                           ((input.max as? Number)?.toFloat() ?: 100f)
                                
                                Slider(
                                    value = currentValue,
                                    onValueChange = { newValue ->
                                        results[input.id] = newValue
                                    },
                                    valueRange = range,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                
                                // Show labels if provided
                                if (input.bottomLabel != null || input.topLabel != null) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = input.bottomLabel ?: "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                        Text(
                                            text = input.topLabel ?: "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }
                        
                        else -> {
                            // For other input types, show a placeholder
                            Text(
                                text = "${input.label}: ${input.type} (not implemented)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // Validate required fields
                    val missingRequired = inputs
                        .filter { it.required }
                        .any { !results.containsKey(it.id) || results[it.id] == null }
                    
                    if (!missingRequired) {
                        onConfirm(results.toMap())
                    }
                },
                enabled = inputs
                    .filter { it.required }
                    .all { results.containsKey(it.id) && results[it.id] != null }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Helper function to format slider values for intensity
private fun formatSliderValue(value: Float, input: QuickAddInput): String {
    // For intensity slider (0 to 1 range)
    return if (input.label.contains("Intensity", ignoreCase = true)) {
        when {
            value < 0.2f -> "Light"
            value < 0.4f -> "Easy"
            value < 0.6f -> "Moderate"
            value < 0.8f -> "Hard"
            else -> "Extreme"
        }
    } else {
        value.roundToInt().toString()
    }
}
