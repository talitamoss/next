package com.domain.app.ui.components.plugin.quickadd

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.Button
import kotlinx.coroutines.delay
import com.domain.app.core.plugin.InputType
import com.domain.app.core.plugin.Plugin
import com.domain.app.core.plugin.QuickAddConfig
import com.domain.app.core.plugin.QuickOption
import com.domain.app.ui.components.core.carousel.Carousel
import com.domain.app.ui.components.core.carousel.CarouselOption
import com.domain.app.ui.components.core.sliders.RangeSlider
import com.domain.app.ui.components.core.sliders.RangeSliderDefaults
import com.domain.app.ui.components.core.sliders.VerticalSlider
import com.domain.app.ui.components.core.button.RecordButton
import com.domain.app.plugins.AudioPlugin
import kotlin.math.roundToInt

/**
 * Quick add dialog that adapts to different plugin input types
 */
@Composable
fun QuickAddDialog(
    plugin: Plugin,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, Any>) -> Unit
) {
    val config = plugin.getQuickAddConfig()
    
    if (config == null) {
        // Plugin doesn't support quick add
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Not Supported") },
            text = { Text("This plugin doesn't support quick add") },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("OK")
                }
            }
        )
    } else {
        // Check if this is a multi-stage or composite input
        when {
            config.stages != null && config.stages.isNotEmpty() -> {
                MultiStageQuickAddContent(
                    plugin = plugin,
                    config = config,
                    onDismiss = onDismiss,
                    onConfirm = onConfirm
                )
            }
            config.inputs != null && config.inputs.isNotEmpty() -> {
                CompositeQuickAddContent(
                    plugin = plugin,
                    config = config,
                    onDismiss = onDismiss,
                    onConfirm = onConfirm
                )
            }
            else -> {
                // Single input based on type
                when (config.inputType) {
                    InputType.VERTICAL_SLIDER -> {
                        VerticalSliderQuickAddContent(
                            plugin = plugin,
                            config = config,
                            onDismiss = onDismiss,
                            onConfirm = onConfirm
                        )
                    }
                    InputType.HORIZONTAL_SLIDER -> {
                        HorizontalSliderQuickAddContent(
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
		    InputType.BUTTON -> {
		        ButtonQuickAddContent(
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
                // Current value display
                Text(
                    text = "${value.roundToInt()} ${config.unit ?: ""}",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Slider
                Slider(
                    value = value,
                    onValueChange = { value = it },
                    valueRange = range,
                    steps = if (config.step != null) {
                        ((range.endInclusive - range.start) / config.step.toFloat()).toInt() - 1
                    } else 0,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Min/Max labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${range.start.roundToInt()}",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = "${range.endInclusive.roundToInt()}",
                        style = MaterialTheme.typography.labelSmall
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
    
    val range = ((config.min as? Number)?.toFloat() ?: 0f)..((config.max as? Number)?.toFloat() ?: 5f)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(config.title)
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                contentAlignment = Alignment.Center
            ) {
                VerticalSlider(
                    value = value,
                    onValueChange = { value = it },
                    valueRange = range,
                    steps = if (config.step != null) {
                        ((range.endInclusive - range.start) / config.step.toFloat()).toInt() - 1
                    } else 0,
                    showLabel = config.showValue,  // FIX 1: Changed from showValue to showLabel
                    labelFormatter = { v ->  // FIX 2: Changed from formatValue to labelFormatter
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
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedOption = option },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedOption == option) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        )
                    ) {
                        Text(
                            text = option.label,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
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
                label = { Text(config.placeholder ?: "Enter value") },
                suffix = { config.unit?.let { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val numericValue = value.toFloatOrNull()
                    if (numericValue != null) {
                        onConfirm(mapOf(config.id to numericValue))
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
 * Button quick add for simple affirmative actions
 * Permissions are already granted when this dialog opens (per the framework)
 */
@Composable
private fun ButtonQuickAddContent(
    plugin: Plugin,
    config: QuickAddConfig,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, Any>) -> Unit
) {
    // Special handling for audio plugin - needs stateful recording UI
    if (plugin.id == "audio") {
        var isRecording by remember { mutableStateOf(false) }
        var isProcessing by remember { mutableStateOf(false) }
        val audioPlugin = plugin as? AudioPlugin

	val context = LocalContext.current

LaunchedEffect(audioPlugin) {
    audioPlugin?.initialize(context)
}
        
        // Handle the recording completion
        fun handleRecordingComplete() {
            if (audioPlugin != null) {
                val dataPoint = audioPlugin.stopRecording()
                if (dataPoint != null) {
                    isProcessing = true
                    // Pass the completed DataPoint to onConfirm
                    onConfirm(mapOf("recorded_data_point" to dataPoint))
                    // The LaunchedEffect below will handle the dismiss
                } else {
                    // Recording failed, just close
                    onDismiss()
                }
            }
        }
        
        // Handle auto-dismiss after processing
        if (isProcessing) {
            LaunchedEffect(Unit) {
                delay(1000) // Wait for save animation
                onDismiss()
            }
        }
        
        AlertDialog(
            onDismissRequest = {
                // Prevent dismissal while recording or processing
                if (!isRecording && !isProcessing) {
                    onDismiss()
                }
            },
            properties = DialogProperties(
                dismissOnBackPress = !isRecording && !isProcessing,
                dismissOnClickOutside = !isRecording && !isProcessing
            ),
            title = {
                Text(config.title)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    RecordButton(
                        isRecording = isRecording,
                        enabled = !isProcessing,
                        onToggle = {
                            if (!isRecording) {
                                // Start recording directly in the plugin
                                if (audioPlugin != null && audioPlugin.startRecording()) {
                                    isRecording = true
                                    // DO NOT call onConfirm here - this would close the dialog!
                                } else {
                                    // Failed to start recording
                                    // Could show an error toast/snackbar here
                                }
                            } else {
                                // Stop recording and save
                                isRecording = false
                                handleRecordingComplete()
                            }
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = when {
                            isProcessing -> "Saving recording..."
                            isRecording -> "Recording... Tap to stop"
                            else -> "Tap to start recording"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {}, // No confirm button for audio
            dismissButton = {
                if (!isRecording && !isProcessing) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }
        )
        return
    }
    
    // Standard button implementation for other button-type plugins
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(config.title)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        onConfirm(mapOf("action" to "confirm"))
                        onDismiss()
                    }
                ) {
                    Text(config.buttonText ?: "Confirm")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
/**
 * Time range quick add using RangeSlider
 * FULLY FLEXIBLE VERSION - All configuration comes from the plugin
 * 
 * The plugin provides ALL necessary information via config.metadata map:
 * - "startKey": Key for start time in data map (default: "start_time")
 * - "endKey": Key for end time in data map (default: "end_time")
 * - "startLabel": Display label for start time (default: "Start")
 * - "endLabel": Display label for end time (default: "End")
 * - "durationLabel": Format for duration display (default: "{duration} duration")
 * - "defaultStartTime": Default start time (default: from defaultValue map or 9f)
 * - "defaultEndTime": Default end time (default: from defaultValue map or 17f)
 * - "headerText": Text to show above the slider (optional)
 * - "durationUnit": Unit for duration (default: "h" for hours)
 * - "showDuration": Whether to show duration card (default: true)
 * - "timeFormat": "24h" or "12h" (default: "24h")
 */
@Composable
private fun TimeRangeQuickAddContent(
    plugin: Plugin,
    config: QuickAddConfig,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, Any>) -> Unit
) {
    // Extract ALL configuration from the plugin's metadata
    // This allows plugins to have complete control without modifying this file
    val metadata = (config.metadata as? Map<String, Any>) ?: emptyMap()
    
    // Keys for data map - plugins can specify custom keys
    val startKey = metadata["startKey"] as? String ?: "start_time"
    val endKey = metadata["endKey"] as? String ?: "end_time"
    
    // Display labels - plugins can specify custom labels
    val startLabel = metadata["startLabel"] as? String ?: "Start"
    val endLabel = metadata["endLabel"] as? String ?: "End"
    
    // Duration display format - plugins can customize this
    val durationLabel = metadata["durationLabel"] as? String ?: "{duration} duration"
    val durationUnit = metadata["durationUnit"] as? String ?: "h"
    val showDuration = metadata["showDuration"] as? Boolean ?: true
    
    // Header text (optional)
    val headerText = metadata["headerText"] as? String
    
    // Time format preference
    val timeFormat = metadata["timeFormat"] as? String ?: "24h"
    
    // Initialize times from config with flexible key handling
    var startTime by remember {
        mutableStateOf(
            when (val default = config.defaultValue) {
                is Map<*, *> -> {
                    // Try the plugin-specified key first
                    (default[startKey] as? Number)?.toFloat()
                        // Then try common alternatives
                        ?: (default["start_time"] as? Number)?.toFloat()
                        ?: (default["startTime"] as? Number)?.toFloat()
                        ?: (default["start"] as? Number)?.toFloat()
                        // Plugin-specific fallback from metadata
                        ?: (metadata["defaultStartTime"] as? Number)?.toFloat()
                        ?: 9f  // Final fallback
                }
                is Number -> default.toFloat()
                else -> (metadata["defaultStartTime"] as? Number)?.toFloat() ?: 9f
            }
        )
    }
    
    var endTime by remember {
        mutableStateOf(
            when (val default = config.defaultValue) {
                is Map<*, *> -> {
                    // Try the plugin-specified key first
                    (default[endKey] as? Number)?.toFloat()
                        // Then try common alternatives
                        ?: (default["end_time"] as? Number)?.toFloat()
                        ?: (default["endTime"] as? Number)?.toFloat()
                        ?: (default["end"] as? Number)?.toFloat()
                        // Plugin-specific fallback from metadata
                        ?: (metadata["defaultEndTime"] as? Number)?.toFloat()
                        ?: 17f  // Final fallback
                }
                is Number -> default.toFloat()
                else -> (metadata["defaultEndTime"] as? Number)?.toFloat() ?: 17f
            }
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
                // Optional header text from plugin
                headerText?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                // Duration display (if enabled by plugin)
                if (showDuration) {
                    val duration = if (endTime > startTime) {
                        endTime - startTime
                    } else {
                        (24 - startTime) + endTime
                    }
                    
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = formatDurationText(duration, durationLabel, durationUnit),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                
                // Time display with plugin-specified labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("$startLabel: ${formatTimeDisplay(startTime, timeFormat)}")
                    Text("$endLabel: ${formatTimeDisplay(endTime, timeFormat)}")
                }
                
                // FIX 3: Declare variables BEFORE RangeSlider
                val minValue = (config.min as? Number)?.toFloat() ?: 0f
                val maxValue = (config.max as? Number)?.toFloat() ?: 48f
                
                // RangeSlider for time selection
                RangeSlider(
                    startValue = startTime,
                    endValue = if (endTime < startTime) endTime + 24f else endTime,
                    onRangeChange = { start, end ->
                        startTime = start % 24
                        endTime = if (end > 24) end % 24 else end
                    },
                    valueRange = minValue..maxValue,  // Now using the pre-declared variables
                    steps = if (config.step != null && config.step.toFloat() > 0) {
                        val range = maxValue - minValue
                        (range / config.step.toFloat()).toInt()
                    } else {
                        47  // Default to hourly steps
                    },
                    minRange = config.step?.toFloat() ?: 1f,
                    showLabels = false,
                    modifier = Modifier.fillMaxWidth(),
                    colors = if (config.primaryColor != null && config.secondaryColor != null) {
                        RangeSliderDefaults.colors(
                            thumbColor = Color(android.graphics.Color.parseColor(config.primaryColor)),
                            activeTrackColor = Color(android.graphics.Color.parseColor(config.primaryColor)),
                            inactiveTrackColor = Color(android.graphics.Color.parseColor(config.secondaryColor))
                        )
                    } else {
                        RangeSliderDefaults.colors()
                    }
                )
                
                // Time markers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Allow plugins to customize time markers via metadata
                    val markers = metadata["timeMarkers"] as? List<String> ?: listOf(
                        "12PM", "6PM", "12AM", "6AM", "12PM"
                    )
                    markers.forEach { marker ->
                        Text(marker, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // Use the plugin-specified keys for the result map
                    val resultMap = mapOf(
                        startKey to startTime,
                        endKey to endTime
                    )
                    onConfirm(resultMap)
                }
            ) {
                Text(metadata["confirmButtonText"] as? String ?: "Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(metadata["cancelButtonText"] as? String ?: "Cancel")
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
                // FIX 4: LinearProgressIndicator needs lambda syntax
                LinearProgressIndicator(
                    progress = { (currentStageIndex + 1f) / stages.size },
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
                            onValueChange = { stageValue = it },
                            label = { Text(currentStage.placeholder ?: "Enter number") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    else -> {
                        Text("Input type not supported in multi-stage")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // Save current stage result
                    results[currentStage.id] = stageValue
                    
                    if (currentStageIndex < stages.size - 1) {
                        // Move to next stage
                        currentStageIndex++
                    } else {
                        // Complete all stages
                        onConfirm(results.toMap())
                    }
                }
            ) {
                Text(if (currentStageIndex < stages.size - 1) "Next" else "Complete")
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
 * Composite quick add for multiple inputs on one screen
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
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.animateContentSize()
            ) {
                inputs.forEach { input ->
                    // Render each input based on its type
                    when (input.type) {
                        InputType.HORIZONTAL_SLIDER -> {
                            var value by remember { 
                                mutableStateOf((input.defaultValue as? Number)?.toFloat() ?: 0f)
                            }
                            results[input.id] = value
                            
                            Column {
                                // Label for the slider
                                Text(
                                    text = input.label,
                                    style = MaterialTheme.typography.labelMedium
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // Show value for hours slider, hide for feeling slider
                                if (input.id == "hours") {
                                    Text(
                                        text = "${String.format("%.1f", value)} ${input.unit ?: ""}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                                
                                // The slider itself
                                Slider(
                                    value = value,
                                    onValueChange = { 
                                        value = it
                                        results[input.id] = it
                                    },
                                    valueRange = (input.min?.toFloat() ?: 0f)..(input.max?.toFloat() ?: 100f),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = MaterialTheme.colorScheme.primary,
                                        inactiveTrackColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                )
                                
                                // Labels for horizontal sliders
                                // For horizontal sliders: bottomLabel = left, topLabel = right
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Left label (low values)
                                    Text(
                                        text = input.bottomLabel ?: "${input.min?.toFloat()?.let { 
                                            if (it == it.toInt().toFloat()) it.toInt().toString() 
                                            else String.format("%.1f", it)
                                        } ?: "0"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    
                                    // Optional center value display (only if no custom labels)
                                    if (input.bottomLabel == null && input.topLabel == null) {
                                        Text(
                                            text = "${value.roundToInt()}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                    
                                    // Right label (high values)
                                    Text(
                                        text = input.topLabel ?: "${input.max?.toFloat()?.let { 
                                            if (it == it.toInt().toFloat()) it.toInt().toString() 
                                            else String.format("%.1f", it)
                                        } ?: "100"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

			  InputType.CAROUSEL -> {
                            var selectedOption by remember { 
                                mutableStateOf(
                                    input.options?.find { it.value == input.defaultValue }
                                        ?: input.options?.firstOrNull()
                                )
                            }
                            results[input.id] = selectedOption?.value ?: ""
                            
                            Column {
                                Text(
                                    text = input.label,
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                input.options?.let { options ->
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
                                            results[input.id] = carouselOption.value
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        height = 80.dp  // Standard height for composite carousel
                                    )
                                }
                            }
                        }
			
			InputType.CHOICE -> {
    // Special handling for food plugin's dynamic food categories
    val dynamicOptions = if (plugin.id == "food" && input.id == "foodCategory") {
        // Get the selected meal type from results
        val mealType = results["mealType"] as? String ?: "snack"
        
        // Return appropriate food options based on meal type
        when (mealType) {
            "snack" -> listOf(
                QuickOption("Nuts", "nuts"),
                QuickOption("Fruit", "fruit"),
                QuickOption("Baked Goods", "baked_goods"),
                QuickOption("Protein Bar", "protein_bar"),
                QuickOption("Chips", "chips"),
                QuickOption("Candy", "candy"),
                QuickOption("Yogurt", "yogurt"),
                QuickOption("Vegetables", "vegetables"),
                QuickOption("Cheese", "cheese")
            )
            "light_meal" -> listOf(
                QuickOption("Carb Heavy", "carb_heavy"),
                QuickOption("Protein Focused", "protein_focused"),
                QuickOption("Salad", "salad"),
                QuickOption("Soup", "soup"),
                QuickOption("Sandwich", "sandwich"),
                QuickOption("Smoothie", "smoothie"),
                QuickOption("Leftovers", "leftovers"),
                QuickOption("Fast Food", "fast_food")
            )
            "full_meal" -> listOf(
                QuickOption("Balanced", "balanced"),
                QuickOption("Protein Heavy", "protein_heavy"),
                QuickOption("Vegetarian", "vegetarian"),
                QuickOption("Vegan", "vegan"),
                QuickOption("Pasta/Rice", "pasta_rice"),
                QuickOption("Pizza", "pizza"),
                QuickOption("Restaurant", "restaurant"),
                QuickOption("Home Cooked", "home_cooked"),
                QuickOption("Takeout", "takeout")
            )
            else -> input.options ?: emptyList()
        }
    } else {
        input.options ?: emptyList()
    }
    
    var selectedOption by remember(dynamicOptions) { 
        mutableStateOf(
            dynamicOptions.find { it.value == input.defaultValue }
                ?: dynamicOptions.firstOrNull()
        )
    }
    results[input.id] = selectedOption?.value ?: ""
    
    Column {
        Text(
            text = input.label,
            style = MaterialTheme.typography.labelMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        // Choice tiles in a row (or grid for many options)
        if (dynamicOptions.size <= 3) {
            // Single row for few options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                dynamicOptions.forEach { option ->
                    Card(
                        onClick = { 
                            selectedOption = option
                            results[input.id] = option.value
                        },
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedOption == option) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        ),
                        border = if (selectedOption == option) {
                            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        } else {
                            BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        } else {
            // Grid layout for many options - using Column with Rows
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                dynamicOptions.chunked(3).forEach { rowOptions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowOptions.forEach { option ->
                            Card(
                                onClick = { 
                                    selectedOption = option
                                    results[input.id] = option.value
                                },
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedOption == option) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    }
                                ),
                                border = if (selectedOption == option) {
                                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                } else {
                                    BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = option.label,
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                        // Fill empty cells if needed
                        repeat(3 - rowOptions.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}                        

                        
                        InputType.TEXT -> {
                            var textValue by remember { 
                                mutableStateOf(input.defaultValue?.toString() ?: "")
                            }
                            results[input.id] = textValue
                            
                            OutlinedTextField(
                                value = textValue,
                                onValueChange = { 
                                    textValue = it
                                    results[input.id] = it
                                },
                                label = { Text(input.label) },
                                placeholder = { Text(input.placeholder ?: "") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        
                        else -> {
                            // Fallback for unsupported types
                            Text("Input type ${input.type} not supported in composite mode")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // Ensure all required fields are filled
                    val missingRequired = inputs.filter { it.required }
                        .any { input -> !results.containsKey(input.id) }
                    
                    if (!missingRequired) {
                        onConfirm(results.toMap())
                    }
                },
                enabled = inputs.filter { it.required }
                    .all { input -> results.containsKey(input.id) }
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
 * Helper function to format duration text with plugin template
 */
private fun formatDurationText(duration: Float, template: String, unit: String): String {
    val hours = duration.toInt()
    val minutes = ((duration - hours) * 60).toInt()
    
    return when {
        // If template contains {hours} and {minutes}, use both
        template.contains("{hours}") && template.contains("{minutes}") -> {
            template
                .replace("{hours}", hours.toString())
                .replace("{minutes}", minutes.toString())
        }
        // If template contains only {duration}, format based on unit
        template.contains("{duration}") -> {
            val durationText = when (unit) {
                "h" -> {
                    if (minutes > 0) "${hours}h ${minutes}m" else "${hours}h"
                }
                "hours" -> {
                    if (minutes > 0) "$hours hours $minutes minutes" else "$hours hours"
                }
                else -> {
                    if (minutes > 0) "${hours}h ${minutes}m" else "${hours}h"
                }
            }
            template.replace("{duration}", durationText)
        }
        // Fallback to simple format
        else -> {
            if (minutes > 0) "${hours}h ${minutes}m $template" else "${hours}h $template"
        }
    }
}

/**
 * Helper function to format time display based on format preference
 */
private fun formatTimeDisplay(time: Float, format: String): String {
    val hours = time.toInt() % 24
    val minutes = ((time - time.toInt()) * 60).toInt()
    
    return when (format) {
        "12h" -> {
            val period = if (hours < 12) "AM" else "PM"
            val displayHour = when (hours) {
                0 -> 12
                in 13..23 -> hours - 12
                else -> hours
            }
            if (minutes > 0) {
                "%d:%02d %s".format(displayHour, minutes, period)
            } else {
                "%d:00 %s".format(displayHour, period)
            }
        }
        else -> {
            // 24h format
            "%02d:%02d".format(hours, minutes)
        }
    }
}
