// app/src/main/java/com/domain/app/ui/components/core/sliders/HorizontalSlider.kt
package com.domain.app.ui.components.core.sliders

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Colors configuration for HorizontalSlider
 */
data class HorizontalSliderColors(
    val thumbColor: Color = Color(0xFF667EEA),
    val thumbBorderColor: Color = Color.White,
    val trackColor: Color = Color.Gray.copy(alpha = 0.3f),
    val activeTrackColor: Brush = Brush.horizontalGradient(
        listOf(Color(0xFF667EEA), Color(0xFF764BA2))
    ),
    val labelBackground: Color = Color.Gray.copy(alpha = 0.2f),
    val labelText: Color = Color.White,
    val tickColor: Color = Color.Gray.copy(alpha = 0.5f),
    val markerText: Color = Color.Gray
)

/**
 * Default colors provider
 */
object HorizontalSliderDefaults {
    @Composable
    fun colors(
        thumbColor: Color = MaterialTheme.colorScheme.primary,
        thumbBorderColor: Color = Color.White,
        trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
        activeTrackColor: Brush = Brush.horizontalGradient(
            listOf(Color(0xFF667EEA), Color(0xFF764BA2))
        ),
        labelBackground: Color = MaterialTheme.colorScheme.surface,
        labelText: Color = MaterialTheme.colorScheme.onSurface,
        tickColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        markerText: Color = MaterialTheme.colorScheme.onSurfaceVariant
    ) = HorizontalSliderColors(
        thumbColor = thumbColor,
        thumbBorderColor = thumbBorderColor,
        trackColor = trackColor,
        activeTrackColor = activeTrackColor,
        labelBackground = labelBackground,
        labelText = labelText,
        tickColor = tickColor,
        markerText = markerText
    )
}

/**
 * Quick option for preset values
 */
data class QuickOption(
    val label: String,
    val value: Any,
    val icon: String? = null
)

/**
 * A customizable horizontal slider component for consistent UI across the app
 */
@Composable
fun HorizontalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..100f,
    steps: Int = 0,
    enabled: Boolean = true,
    showLabel: Boolean = true,
    showTicks: Boolean = false,
    showValueMarkers: Boolean = false,
    height: Dp = 48.dp,
    trackHeight: Dp = 6.dp,
    thumbSize: Dp = 20.dp,
    colors: HorizontalSliderColors = HorizontalSliderDefaults.colors(),
    hapticFeedback: Boolean = true,
    labelFormatter: (Float) -> String = { it.roundToInt().toString() },
    markerLabels: Map<Float, String>? = null,
    onValueChangeFinished: (() -> Unit)? = null
) {
    val haptics = LocalHapticFeedback.current
    
    var sliderWidth by remember { mutableStateOf(0) }
    var isDragging by remember { mutableStateOf(false) }
    var lastHapticValue by remember { mutableStateOf(value) }
    
    // Animated value for smooth transitions
    val animatedValue by animateFloatAsState(
        targetValue = value,
        animationSpec = spring(dampingRatio = 0.8f),
        label = "slider_value"
    )
    
    // Calculate normalized position (0 to 1)
    val normalizedValue = ((animatedValue - valueRange.start) / 
                          (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Value label
        if (showLabel) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .alpha(if (isDragging) 1f else 0.9f),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colors.labelBackground
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (isDragging) 4.dp else 1.dp
                    )
                ) {
                    Text(
                        text = labelFormatter(animatedValue),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.labelText
                    )
                }
            }
        }
        
        // Slider track and thumb
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .onSizeChanged { size ->
                    sliderWidth = size.width
                }
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(enabled, valueRange) {
                        if (!enabled) return@pointerInput
                        
                        detectTapGestures(
                            onTap = { offset ->
                                // Allow tapping anywhere on the track to set value
                                val newNormalized = (offset.x / size.width).coerceIn(0f, 1f)
                                val newValue = valueRange.start + 
                                    (newNormalized * (valueRange.endInclusive - valueRange.start))
                                val snappedValue = snapToStep(newValue, valueRange, steps)
                                
                                if (hapticFeedback) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                
                                onValueChange(snappedValue)
                                onValueChangeFinished?.invoke()
                            }
                        )
                    }
                    .pointerInput(enabled, valueRange) {
                        if (!enabled) return@pointerInput
                        
                        detectHorizontalDragGestures(
                            onDragStart = { offset ->
                                isDragging = true
                                if (hapticFeedback) {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                val newNormalized = (offset.x / size.width).coerceIn(0f, 1f)
                                val newValue = valueRange.start + 
                                    (newNormalized * (valueRange.endInclusive - valueRange.start))
                                val snappedValue = snapToStep(newValue, valueRange, steps)
                                onValueChange(snappedValue)
                            },
                            onDragEnd = {
                                isDragging = false
                                onValueChangeFinished?.invoke()
                            }
                        ) { _, dragAmount ->
                            val currentX = size.width * normalizedValue
                            val newX = (currentX + dragAmount).coerceIn(0f, size.width.toFloat())
                            val newNormalized = newX / size.width
                            val newValue = valueRange.start + 
                                (newNormalized * (valueRange.endInclusive - valueRange.start))
                            val snappedValue = snapToStep(newValue, valueRange, steps)
                            
                            // Haptic feedback on step changes
                            if (hapticFeedback && steps > 0) {
                                val stepSize = (valueRange.endInclusive - valueRange.start) / (steps + 1)
                                if (abs(snappedValue - lastHapticValue) >= stepSize * 0.9f) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    lastHapticValue = snappedValue
                                }
                            }
                            
                            onValueChange(snappedValue)
                        }
                    }
            ) {
                val centerY = size.center.y
                val thumbX = size.width * normalizedValue
                val trackHeightPx = trackHeight.toPx()
                val thumbRadius = thumbSize.toPx() / 2
                
                // Draw track background
                drawLine(
                    color = colors.trackColor,
                    start = Offset(thumbRadius, centerY),
                    end = Offset(size.width - thumbRadius, centerY),
                    strokeWidth = trackHeightPx,
                    cap = StrokeCap.Round
                )
                
                // Draw active track with gradient
                drawLine(
                    brush = colors.activeTrackColor,
                    start = Offset(thumbRadius, centerY),
                    end = Offset(thumbX, centerY),
                    strokeWidth = trackHeightPx,
                    cap = StrokeCap.Round
                )
                
                // Draw ticks if enabled
                if (showTicks && steps > 0) {
                    val stepWidth = (size.width - 2 * thumbRadius) / (steps + 1)
                    for (i in 0..steps + 1) {
                        val x = thumbRadius + (i * stepWidth)
                        drawLine(
                            color = colors.tickColor,
                            start = Offset(x, centerY - trackHeightPx),
                            end = Offset(x, centerY + trackHeightPx),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
                
                // Draw thumb shadow
                drawCircle(
                    color = Color.Black.copy(alpha = 0.2f),
                    radius = if (isDragging) thumbRadius + 4 else thumbRadius + 2,
                    center = Offset(thumbX + 2, centerY + 2)
                )
                
                // Draw thumb outer circle (white border)
                drawCircle(
                    color = colors.thumbBorderColor,
                    radius = if (isDragging) thumbRadius + 2 else thumbRadius,
                    center = Offset(thumbX, centerY)
                )
                
                // Draw thumb inner circle
                drawCircle(
                    color = colors.thumbColor,
                    radius = if (isDragging) thumbRadius - 2 else thumbRadius - 3,
                    center = Offset(thumbX, centerY)
                )
            }
        }
        
        // Value markers
        if (showValueMarkers) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (markerLabels != null) {
                    markerLabels.forEach { (_, label) ->
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            color = colors.markerText,
                            textAlign = TextAlign.Center
                        )
                    }
                } else if (steps > 0) {
                    // Generate evenly spaced markers
                    repeat(steps + 2) { index ->
                        val markerValue = valueRange.start + 
                            (index * (valueRange.endInclusive - valueRange.start) / (steps + 1))
                        Text(
                            text = labelFormatter(markerValue),
                            fontSize = 11.sp,
                            color = colors.markerText,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Horizontal slider with preset buttons
 */
@Composable
fun HorizontalSliderWithPresets(
    value: Float,
    onValueChange: (Float) -> Unit,
    presets: List<QuickOption>,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..100f,
    showSlider: Boolean = true,
    colors: HorizontalSliderColors = HorizontalSliderDefaults.colors()
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Preset chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { preset ->
                val presetValue = (preset.value as? Number)?.toFloat() ?: 0f
                FilterChip(
                    selected = abs(value - presetValue) < 0.01f,
                    onClick = { onValueChange(presetValue) },
                    label = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            preset.icon?.let { icon ->
                                Text(text = icon, fontSize = 16.sp)
                            }
                            Text(text = preset.label)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        // Optional slider for fine-tuning
        if (showSlider) {
            HorizontalSlider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                colors = colors,
                showLabel = true,
                showValueMarkers = false
            )
        }
    }
}

private fun snapToStep(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int
): Float {
    if (steps <= 0) return value
    
    val stepSize = (valueRange.endInclusive - valueRange.start) / (steps + 1)
    val stepIndex = ((value - valueRange.start) / stepSize).roundToInt()
    return (valueRange.start + stepIndex * stepSize).coerceIn(valueRange)
}
