package com.domain.app.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepQuickAddDialog(
    onDismiss: () -> Unit,
    onConfirm: (Map<String, Any>) -> Unit
) {
    var durationHours by remember { mutableStateOf(8f) }  // 0-14 hours
    var quality by remember { mutableStateOf(50f) }  // 0-100 scale
    var dreamNotes by remember { mutableStateOf("") }
    var showDreamField by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "Log Your Sleep",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Dual vertical sliders
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Duration Slider
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Duration",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .fillMaxHeight()
                                .weight(1f)
                        ) {
                            // Labels on the left
                            Column(
                                modifier = Modifier.align(Alignment.CenterStart),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "14h",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = "0h",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            // Slider track
                            Box(
                                modifier = Modifier
                                    .width(60.dp)
                                    .fillMaxHeight()
                                    .align(Alignment.Center)
                                    .clip(RoundedCornerShape(30.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                                MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        )
                                    )
                            ) {
                                SleepDurationSlider(
                                    value = durationHours,
                                    onValueChange = { newValue ->
                                        durationHours = newValue
                                        showDreamField = true
                                    },
                                    valueRange = 0f..14f,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        
                        // Duration display
                        Card(
                            modifier = Modifier.padding(top = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Text(
                                text = formatDuration(durationHours),
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                    
                    // Quality Slider
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Quality",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .fillMaxHeight()
                                .weight(1f)
                        ) {
                            // Labels on the right
                            Column(
                                modifier = Modifier.align(Alignment.CenterEnd),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Great",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = "Poor",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            
                            // Slider track
                            Box(
                                modifier = Modifier
                                    .width(60.dp)
                                    .fillMaxHeight()
                                    .align(Alignment.Center)
                                    .clip(RoundedCornerShape(30.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primaryContainer,
                                                MaterialTheme.colorScheme.tertiaryContainer
                                            )
                                        )
                                    )
                            ) {
                                SleepQualitySlider(
                                    value = quality,
                                    onValueChange = { newValue ->
                                        quality = newValue
                                        showDreamField = true
                                    },
                                    valueRange = 0f..100f,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        
                        // Quality display
                        Card(
                            modifier = Modifier.padding(top = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "${quality.roundToInt()}%",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = getQualityDescription(quality.roundToInt()),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
                
                // Dream journal field (optional)
                if (showDreamField) {
                    OutlinedTextField(
                        value = dreamNotes,
                        onValueChange = { dreamNotes = it },
                        label = { Text("Dream journal (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 3,
                        placeholder = { Text("Remember any dreams?") }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val sleepData = mutableMapOf<String, Any>(
                        "sleep_data" to mapOf(
                            "duration" to (durationHours * 60).roundToInt(),  // Convert to minutes
                            "quality" to quality.roundToInt()
                        )
                    )
                    if (dreamNotes.isNotBlank()) {
                        sleepData["dream"] = dreamNotes
                    }
                    onConfirm(sleepData)
                }
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

@Composable
private fun SleepDurationSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..14f
) {
    BoxWithConstraints(
        modifier = modifier
    ) {
        val height = constraints.maxHeight.toFloat()
        val thumbY = remember(value, height, valueRange) {
            val normalized = (value - valueRange.start) / (valueRange.endInclusive - valueRange.start)
            height * (1f - normalized)  // Invert for top = max
        }
        
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(valueRange) {
                    detectVerticalDragGestures { change, _ ->
                        val newY = change.position.y.coerceIn(0f, height)
                        val normalized = 1f - (newY / height)  // Invert for top = max
                        val newValue = valueRange.start + 
                            (normalized * (valueRange.endInclusive - valueRange.start))
                        onValueChange(newValue.coerceIn(valueRange))
                    }
                }
        ) {
            val centerX = size.width / 2
            
            // Track
            drawLine(
                color = Color.Gray.copy(alpha = 0.3f),
                start = Offset(centerX, 20f),
                end = Offset(centerX, size.height - 20f),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
            
            // Active track
            drawLine(
                color = Color.Gray.copy(alpha = 0.6f),
                start = Offset(centerX, thumbY),
                end = Offset(centerX, size.height - 20f),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
            
            // Thumb
            drawCircle(
                color = Color.White,
                radius = 12.dp.toPx(),
                center = Offset(centerX, thumbY)
            )
            drawCircle(
                color = Color.Gray,
                radius = 10.dp.toPx(),
                center = Offset(centerX, thumbY)
            )
        }
    }
}

@Composable
private fun SleepQualitySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..100f
) {
    BoxWithConstraints(
        modifier = modifier
    ) {
        val height = constraints.maxHeight.toFloat()
        val thumbY = remember(value, height, valueRange) {
            val normalized = (value - valueRange.start) / (valueRange.endInclusive - valueRange.start)
            height * (1f - normalized)  // Invert for top = max
        }
        
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(valueRange) {
                    detectVerticalDragGestures { change, _ ->
                        val newY = change.position.y.coerceIn(0f, height)
                        val normalized = 1f - (newY / height)  // Invert for top = max
                        val newValue = valueRange.start + 
                            (normalized * (valueRange.endInclusive - valueRange.start))
                        onValueChange(newValue.coerceIn(valueRange))
                    }
                }
        ) {
            val centerX = size.width / 2
            
            // Track
            drawLine(
                color = Color.Gray.copy(alpha = 0.3f),
                start = Offset(centerX, 20f),
                end = Offset(centerX, size.height - 20f),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
            
            // Active track
            drawLine(
                color = Color.Gray.copy(alpha = 0.6f),
                start = Offset(centerX, thumbY),
                end = Offset(centerX, size.height - 20f),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
            
            // Thumb
            drawCircle(
                color = Color.White,
                radius = 12.dp.toPx(),
                center = Offset(centerX, thumbY)
            )
            drawCircle(
                color = Color.Gray,
                radius = 10.dp.toPx(),
                center = Offset(centerX, thumbY)
            )
        }
    }
}

private fun formatDuration(hours: Float): String {
    val totalMinutes = (hours * 60).roundToInt()
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return when {
        h == 0 && m > 0 -> "${m}m"
        m == 0 -> "${h}h"
        else -> "${h}h ${m}m"
    }
}

private fun getQualityDescription(value: Int) = when {
    value >= 80 -> "Excellent"
    value >= 60 -> "Good"
    value >= 40 -> "Fair"
    value >= 20 -> "Poor"
    else -> "Terrible"
}
