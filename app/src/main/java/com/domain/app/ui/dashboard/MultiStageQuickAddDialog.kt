// app/src/main/java/com/domain/app/ui/dashboard/MultiStageQuickAddDialog.kt
package com.domain.app.ui.dashboard

import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.domain.app.core.plugin.*
import com.domain.app.ui.components.core.sliders.HorizontalSlider
import com.domain.app.ui.components.core.sliders.VerticalSlider
import com.domain.app.ui.components.core.input.ValidatedTextField
import com.domain.app.ui.components.core.input.NumberTextField
import com.domain.app.ui.components.core.feedback.LoadingButton

/**
 * Refactored MultiStageQuickAddDialog using the new component library.
 * This eliminates all the duplicate code from the original implementation.
 */
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
    var isProcessing by remember { mutableStateOf(false) }
    
    if (currentStage == null) {
        LaunchedEffect(Unit) {
            onComplete(collectedData.toMap())
        }
        return
    }
    
    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        title = { 
            Column {
                Text(
                    text = plugin.metadata.name,
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                // Progress indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Step ${currentStageIndex + 1} of ${stages.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Visual progress dots
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        stages.indices.forEach { index ->
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .background(
                                        if (index <= currentStageIndex) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    )
                            )
                        }
                    }
                }
            }
        },
        text = {
            AnimatedContent(
                targetState = currentStage,
                transitionSpec = {
                    if (targetState.id > initialState.id) {
                        slideInHorizontally { it } + fadeIn() togetherWith
                        slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith
                        slideOutHorizontally { it } + fadeOut()
                    }
                },
                label = "stage_content"
            ) { stage ->
                StageContent(
                    stage = stage,
                    onValueChange = { value ->
                        if (value != null) {
                            collectedData[stage.id] = value
                        } else {
                            collectedData.remove(stage.id)
                        }
                    },
                    currentValue = collectedData[stage.id]
                )
            }
        },
        confirmButton = {
            LoadingButton(
                onClick = {
                    if (currentStageIndex < stages.size - 1) {
                        currentStageIndex++
                    } else {
                        isProcessing = true
                        onComplete(collectedData.toMap())
                    }
                },
                enabled = !currentStage.required || collectedData.containsKey(currentStage.id),
                isLoading = isProcessing,
                text = if (currentStageIndex < stages.size - 1) "Next" else "Complete",
                loadingText = "Saving..."
            )
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (currentStageIndex > 0) {
                    TextButton(
                        onClick = { currentStageIndex-- },
                        enabled = !isProcessing
                    ) {
                        Text("Back")
                    }
                }
                TextButton(
                    onClick = onDismiss,
                    enabled = !isProcessing
                ) {
                    Text("Cancel")
                }
            }
        }
    )
}

/**
 * Content for each stage, using our new component library
 */
@Composable
private fun StageContent(
    stage: QuickAddStage,
    onValueChange: (Any?) -> Unit,
    currentValue: Any?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Stage title and description
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stage.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            
            stage.hint?.let { hint ->
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Input based on type
        when (stage.inputType) {
            InputType.TEXT -> {
                ValidatedTextField(
                    value = currentValue?.toString() ?: "",
                    onValueChange = { onValueChange(if (it.isNotBlank()) it else null) },
                    label = stage.hint ?: "Enter value",
                    required = stage.required,
                    maxLines = 3,
                    singleLine = false
                )
            }
            
            InputType.NUMBER -> {
                NumberTextField(
                    value = currentValue?.toString() ?: "",
                    onValueChange = { value ->
                        val number = value.toDoubleOrNull()
                        onValueChange(number)
                    },
                    label = stage.hint ?: "Enter number",
                    required = stage.required,
                    min = (stage.min as? Number)?.toDouble(),
                    max = (stage.max as? Number)?.toDouble()
                )
            }
            
            InputType.SLIDER -> {
                val range = when {
                    stage.min != null && stage.max != null -> {
                        (stage.min as Number).toFloat()..(stage.max as Number).toFloat()
                    }
                    else -> 0f..100f
                }
                
                val sliderValue = (currentValue as? Number)?.toFloat() ?: 
                                 (stage.defaultValue as? Number)?.toFloat() ?: 
                                 range.start
                
                Column {
                    HorizontalSlider(
                        value = sliderValue,
                        onValueChange = { onValueChange(it) },
                        valueRange = range,
                        steps = (stage.step as? Number)?.toInt() ?: 0,
                        showLabel = true,
                        showTicks = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Value display
                    Text(
                        text = stage.formatValue?.invoke(sliderValue) ?: sliderValue.toInt().toString(),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
            
            InputType.CHOICE -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    stage.options?.forEach { option ->
                        FilterChip(
                            selected = currentValue == option.value,
                            onClick = { onValueChange(option.value) },
                            label = { 
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    option.icon?.let { Text(it) }
                                    Text(option.label)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            
            InputType.SCALE -> {
                val scaleValue = (currentValue as? Number)?.toInt() ?: 
                                (stage.defaultValue as? Number)?.toInt() ?: 3
                
                Column {
                    // Visual scale selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        (1..5).forEach { value ->
                            FilterChip(
                                selected = scaleValue == value,
                                onClick = { onValueChange(value) },
                                label = {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = getScaleEmoji(value),
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                        stage.scaleLabels?.get(value)?.let { label ->
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }
                    
                    // Current selection display
                    stage.scaleLabels?.get(scaleValue)?.let { label ->
                        Text(
                            text = "Selected: $label",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(top = 8.dp)
                        )
                    }
                }
            }
            
            InputType.DURATION -> {
                DurationInput(
                    initialMinutes = (currentValue as? Int) ?: 0,
                    onDurationChange = { minutes ->
                        onValueChange(if (minutes > 0) minutes else null)
                    },
                    required = stage.required
                )
            }
            
            else -> {
                // Fallback for unknown types
                ValidatedTextField(
                    value = currentValue?.toString() ?: "",
                    onValueChange = { onValueChange(if (it.isNotBlank()) it else null) },
                    label = stage.hint ?: "Enter value",
                    required = stage.required
                )
            }
        }
    }
}

/**
 * Duration input component for time-based inputs
 */
@Composable
private fun DurationInput(
    initialMinutes: Int,
    onDurationChange: (Int) -> Unit,
    required: Boolean
) {
    val hours = initialMinutes / 60
    val minutes = initialMinutes % 60
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NumberTextField(
            value = if (hours > 0) hours.toString() else "",
            onValueChange = { value ->
                val h = value.toIntOrNull() ?: 0
                onDurationChange(h * 60 + minutes)
            },
            label = "Hours",
            modifier = Modifier.weight(1f),
            min = 0.0,
            max = 24.0
        )
        
        NumberTextField(
            value = if (minutes > 0) minutes.toString() else "",
            onValueChange = { value ->
                val m = value.toIntOrNull() ?: 0
                onDurationChange(hours * 60 + m)
            },
            label = "Minutes",
            modifier = Modifier.weight(1f),
            min = 0.0,
            max = 59.0
        )
    }
}

/**
 * Helper function to get emoji for scale values
 */
private fun getScaleEmoji(value: Int): String {
    return when (value) {
        1 -> "😢"
        2 -> "😕"
        3 -> "😐"
        4 -> "😊"
        5 -> "😄"
        else -> "❓"
    }
}

// Extension to QuickAddStage for additional properties
data class QuickAddStage(
    val id: String,
    val title: String,
    val inputType: InputType,
    val hint: String? = null,
    val required: Boolean = false,
    val defaultValue: Any? = null,
    val min: Any? = null,
    val max: Any? = null,
    val step: Any? = null,
    val options: List<QuickAddOption>? = null,
    val formatValue: ((Float) -> String)? = null,
    val scaleLabels: Map<Int, String>? = null
)

// Missing data class definition
data class QuickAddOption(
    val label: String,
    val value: Any,
    val icon: String? = null
)
