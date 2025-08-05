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
import androidx.compose.ui.unit.sp
import com.domain.app.core.plugin.QuickOption
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodQuickAddDialog(
    options: List<QuickOption>,  // Keeping for compatibility but not used
    onDismiss: () -> Unit,
    onConfirm: (Int, String?) -> Unit
) {
    var moodValue by remember { mutableStateOf(50f) }  // 0-100 scale
    var note by remember { mutableStateOf("") }
    var showNoteField by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "How are you feeling?",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Vertical mood slider
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Labels on the left
                    Column(
                        modifier = Modifier.padding(end = 16.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "Light",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "Dark",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    
                    // Custom vertical slider
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(40.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.surface,
                                        MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    startY = 0f,
                                    endY = Float.POSITIVE_INFINITY
                                )
                            )
                    ) {
                        MoodVerticalSlider(
                            value = moodValue,
                            onValueChange = { newValue ->
                                moodValue = newValue
                                showNoteField = true
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    
                    // Current value indicator
                    Column(
                        modifier = Modifier.padding(start = 16.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "${moodValue.roundToInt()}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = getMoodDescription(moodValue.roundToInt()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Optional note field
                if (showNoteField) {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Add a note (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 3
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(moodValue.roundToInt(), note.takeIf { it.isNotBlank() })
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
fun MoodVerticalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..100f
) {
    BoxWithConstraints(
        modifier = modifier
    ) {
        val height = constraints.maxHeight.toFloat()
        val thumbY = remember(value, height) {
            val normalized = (value - valueRange.start) / (valueRange.endInclusive - valueRange.start)
            height * (1f - normalized)  // Invert for top = max
        }
        
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, _ -> }
                }
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

private fun getMoodDescription(value: Int) = when {
    value >= 80 -> "Light"
    value >= 60 -> "Bright"
    value >= 40 -> "Neutral"
    value >= 20 -> "Dim"
    else -> "Dark"
}
