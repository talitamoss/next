// app/src/main/java/com/domain/app/ui/components/plugin/quickadd/QuickAddDialog.kt
package com.domain.app.ui.components.plugin.quickadd

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.domain.app.core.plugin.*
import com.domain.app.ui.components.core.sliders.HorizontalSlider
import com.domain.app.ui.components.core.sliders.HorizontalSliderDefaults
import com.domain.app.ui.components.core.sliders.VerticalSlider
import com.domain.app.ui.components.core.sliders.VerticalSliderDefaults
import com.domain.app.ui.components.core.sliders.RangeSlider
import com.domain.app.ui.components.core.sliders.RangeSliderDefaults
import com.domain.app.ui.components.core.carousel.Carousel
import com.domain.app.ui.components.core.carousel.CarouselOption
import com.domain.app.ui.components.core.carousel.CarouselDefaults
import kotlin.math.roundToInt

/**
 * Time range quick add (for sleep tracking)
 */
@Composable
private fun TimeRangeQuickAddContent(
    plugin: Plugin,
    config: QuickAddConfig,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, Any>) -> Unit
) {
    // Get default values or use sensible defaults
    val defaults = config.defaultValue as? Map<*, *>
    var bedtimeHours by remember { 
        mutableStateOf(defaults?.get("bedtime") as? Float ?: 23f) // 11 PM
    }
    var waketimeHours by remember { 
        mutableStateOf(defaults?.get("waketime") as? Float ?: 7f) // 7 AM
    }
    
    // Calculate sleep duration
    val duration = remember(bedtimeHours, waketimeHours) {
        var diff = waketimeHours - bedtimeHours
        if (diff < 0) diff += 24 // Handle overnight sleep
        diff
    }
    
    // Format time for display
    fun formatTime(hours: Float): String {
        val h = hours.toInt()
        val m = ((hours - h) * 60).toInt()
        val period = if (h >= 12) "PM" else "AM"
        val displayHour = when {
            h == 0 -> 12
            h > 12 -> h - 12
            else -> h
        }
        return String.format("%d:%02d %s", displayHour, m, period)
    }
    
    // Format duration for display
    fun formatDuration(hours: Float): String {
        val h = hours.toInt()
        val m = ((hours - h) * 60).toInt()
        return "${h}h ${m}m"
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
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Sleep duration display (prominent)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Total Sleep",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatDuration(duration),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Recommended: 7-9 hours",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Bedtime and Wake time display
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🌙",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = "Bedtime",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatTime(bedtimeHours),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "☀️",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = "Wake up",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatTime(waketimeHours),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // Time range slider
                Column {
                    Text(
                        text = "Adjust sleep schedule",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // Convert to 12PM-12PM range for display
                    val adjustedBedtime = if (bedtimeHours >= 12) bedtimeHours else bedtimeHours + 24
                    val adjustedWaketime = if (waketimeHours >= 12) waketimeHours else waketimeHours + 24
                    
                    RangeSlider(
                        startValue = adjustedBedtime,
                        endValue = adjustedWaketime,
                        onRangeChange = { start, end ->
                            bedtimeHours = if (start >= 24) start - 24 else start
                            waketimeHours = if (end >= 24) end - 24 else end
                        },
                        valueRange = 12f..36f, // 12PM to 12PM next day
                        steps = 47, // 30-minute increments (24 hours * 2 - 1)
                        minRange = 0.5f, // Minimum 30 minutes sleep
                        showLabels = false, // We're showing custom labels above
                        colors = RangeSliderDefaults.colors(
                            activeTrackStartColor = Color(0xFF6B46C1),
                            activeTrackEndColor = Color(0xFF9333EA)
                        )
                    )
                    
                    // Time markers
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("12PM", style = MaterialTheme.typography.labelSmall)
                        Text("6PM", style = MaterialTheme.typography.labelSmall)
                        Text("12AM", style = MaterialTheme.typography.labelSmall)
                        Text("6AM", style = MaterialTheme.typography.labelSmall)
                        Text("12PM", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(mapOf(
                        "bedtime" to bedtimeHours,
                        "waketime" to waketimeHours
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
        // NEW: Check for composite inputs first
        config?.inputs != null -> {
            CompositeQuickAddContent(
                plugin = plugin,
                config = config,
                onDismiss = onDismiss,
                onConfirm = onConfirm
            )
        }
        
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
                InputType.TIME_RANGE -> TimeRangeQuickAddContent(plugin, config, onDismiss, onConfirm)
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
 * Uses config fields for colors and labels - no hard-coded plugin knowledge
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
                // Optional emoji/icon display based on value - ONLY show if showValue is true
                if (config.showValue) {
                    config.options?.find { (it.value as? Number)?.toInt() == value.roundToInt() }?.let { option ->
                        option.icon?.let { icon ->
                            Text(
                                text = icon,
                                style = MaterialTheme.typography.displaySmall
                            )
                        }
                    }
                }
                
                // Container for slider with labels
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Labels on the left side
                    Column(
                        modifier = Modifier.padding(end = 8.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.End
                    ) {
                        // Top label (use topLabel if provided, otherwise max value)
                        Text(
                            text = config.topLabel ?: "${config.max ?: 5}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        // Bottom label (use bottomLabel if provided, otherwise min value)
                        Text(
                            text = config.bottomLabel ?: "${config.min ?: 1}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Parse colors from config or use defaults
                    val sliderColors = if (config.primaryColor != null || config.secondaryColor != null) {
                        VerticalSliderDefaults.colors(
                            thumbColor = config.primaryColor?.let { 
                                try { Color(android.graphics.Color.parseColor(it)) } 
                                catch (e: Exception) { MaterialTheme.colorScheme.primary }
                            } ?: MaterialTheme.colorScheme.primary,
                            activeTrackColor = config.secondaryColor?.let { 
                                try { Color(android.graphics.Color.parseColor(it)) } 
                                catch (e: Exception) { MaterialTheme.colorScheme.primaryContainer }
                            } ?: MaterialTheme.colorScheme.primaryContainer
                        )
                    } else {
                        VerticalSliderDefaults.colors()
                    }
                    
                    // Vertical slider
                    VerticalSlider(
                        value = value,
                        onValueChange = { value = it },
                        valueRange = ((config.min as? Number)?.toFloat() ?: 1f)..((config.max as? Number)?.toFloat() ?: 5f),
                        steps = ((config.max as? Number)?.toFloat()?.minus((config.min as? Number)?.toFloat() ?: 1f))?.toInt()?.minus(1) ?: 3,
                        height = 200.dp,
                        showLabel = config.showValue,  // Use the showValue flag to control numeric display
                        showTicks = true,
                        colors = sliderColors,
                        labelFormatter = { v -> 
                            if (config.showValue) {
                                // If showing values, try to find a label from options, otherwise show number
                                config.options?.find { (it.value as? Number)?.toFloat() == v }?.label 
                                    ?: v.roundToInt().toString()
                            } else {
                                ""  // Don't show any labels if showValue is false
                            }
                        }
                    )
                }
                
                // Show current selection text ONLY if showValue is true
                if (config.showValue) {
                    config.options?.find { (it.value as? Number)?.toInt() == value.roundToInt() }?.let { option ->
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
 * Horizontal slider quick add (for quantities, amounts, etc.)
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Horizontal slider component
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
    val collectedData = remember { mutableStateMapOf<String, Any>() }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(config.title)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                config.inputs?.forEach { input ->
                    CompositeInputItem(
                        input = input,
                        onValueChange = { value ->
                            if (value != null) {
                                collectedData[input.id] = value
                            } else {
                                collectedData.remove(input.id)
                            }
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(collectedData.toMap())
                },
                enabled = config.inputs?.all { input ->
                    !input.required || collectedData.containsKey(input.id)
                } ?: true
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
 * Individual input item for composite layout
 */
@Composable
private fun CompositeInputItem(
    input: QuickAddInput,
    onValueChange: (Any?) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = input.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
        
        when (input.type) {
            InputType.HORIZONTAL_SLIDER -> {
                var sliderValue by remember { 
                    mutableStateOf((input.defaultValue as? Number)?.toFloat() ?: 0f) 
                }
                
                LaunchedEffect(sliderValue) {
                    onValueChange(sliderValue)
                }
                
                Column {
                    HorizontalSlider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        valueRange = ((input.min as? Number)?.toFloat() ?: 0f)..((input.max as? Number)?.toFloat() ?: 100f),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = input.topLabel ?: "${input.min ?: 0}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${sliderValue.roundToInt()}${input.unit?.let { " $it" } ?: ""}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = input.bottomLabel ?: "${input.max ?: 100}",
                            style = MaterialTheme.typography.bodySmall,
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
                
                LaunchedEffect(selectedOption) {
                    onValueChange(selectedOption?.value)
                }
                
                // Use the proper Carousel component
                Carousel(
                    options = input.options?.map { 
                        CarouselOption(it.label, it.value, it.icon)
                    } ?: emptyList(),
                    selectedOption = selectedOption?.let { 
                        CarouselOption(it.label, it.value, it.icon)
                    },
                    onOptionSelected = { carouselOption ->
                        selectedOption = input.options?.find { 
                            it.value == carouselOption.value 
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = when (input.id) {
                        "type" -> CarouselDefaults.exerciseColors()
                        else -> CarouselDefaults.colors()
                    }
                )
            }
            
            InputType.TEXT -> {
                var text by remember { 
                    mutableStateOf(input.defaultValue as? String ?: "") 
                }
                
                LaunchedEffect(text) {
                    onValueChange(text.ifEmpty { null })
                }
                
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text(input.placeholder ?: "") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            InputType.NUMBER -> {
                var number by remember { 
                    mutableStateOf(input.defaultValue?.toString() ?: "") 
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
                    placeholder = { Text(input.placeholder ?: "Enter number") },
                    suffix = { input.unit?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            else -> {
                Text(
                    text = "Unsupported input type: ${input.type}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
