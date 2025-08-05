// app/src/main/java/com/domain/app/ui/components/core/sliders/VerticalSlider.kt
package com.domain.app.ui.components.core.sliders

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * A highly customizable vertical slider component that can be reused across all dialogs.
 * Eliminates the need for duplicate slider implementations in each quick-add dialog.
 * 
 * @param value Current value of the slider
 * @param onValueChange Callback when value changes
 * @param modifier Modifier for the slider container
 * @param valueRange Range of values (default 0f..100f)
 * @param steps Number of discrete steps (0 for continuous)
 * @param enabled Whether the slider is interactive
 * @param showLabel Whether to show the current value label
 * @param showTicks Whether to show tick marks for steps
 * @param height Height of the slider track
 * @param width Width of the slider track
 * @param colors Custom colors for the slider
 * @param hapticFeedback Whether to provide haptic feedback
 * @param labelFormatter Custom formatter for the value label
 */
@Composable
fun VerticalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..100f,
    steps: Int = 0,
    enabled: Boolean = true,
    showLabel: Boolean = true,
    showTicks: Boolean = false,
    height: Dp = 200.dp,
    width: Dp = 60.dp,
    colors: VerticalSliderColors = VerticalSliderDefaults.colors(),
    hapticFeedback: Boolean = true,
    labelFormatter: (Float) -> String = { it.roundToInt().toString() }
) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    
    var sliderHeight by remember { mutableStateOf(0) }
    var isDragging by remember { mutableStateOf(false) }
    
    // Animated value for smooth transitions
    val animatedValue by animateFloatAsState(
        targetValue = value,
        label = "slider_value"
    )
    
    // Calculate normalized position (0 to 1)
    val normalizedValue = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start))
        .coerceIn(0f, 1f)
    
    Box(
        modifier = modifier
            .width(width)
            .height(height),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            // Value label at top
            if (showLabel) {
                Card(
                    modifier = Modifier
                        .alpha(if (isDragging) 1f else 0.8f)
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colors.labelBackground
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (isDragging) 8.dp else 2.dp
                    )
                ) {
                    Text(
                        text = labelFormatter(value),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.labelText
                    )
                }
            }
            
            // Slider track and thumb
            Box(
                modifier = Modifier
                    .weight(1f)
                    .width(width)
                    .onSizeChanged { size ->
                        sliderHeight = size.height
                    }
                    .pointerInput(enabled, valueRange) {
                        if (!enabled) return@pointerInput
                        
                        detectVerticalDragGestures(
                            onDragStart = { offset ->
                                isDragging = true
                                if (hapticFeedback) {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                updateValue(offset.y, sliderHeight, valueRange, steps, onValueChange, haptics, hapticFeedback)
                            },
                            onDragEnd = {
                                isDragging = false
                            }
                        ) { _, dragAmount ->
                            val currentY = sliderHeight * (1f - normalizedValue)
                            val newY = (currentY - dragAmount).coerceIn(0f, sliderHeight.toFloat())
                            updateValue(newY, sliderHeight, valueRange, steps, onValueChange, haptics, hapticFeedback)
                        }
                    }
            ) {
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val trackWidth = width.toPx() * 0.3f
                    val centerX = size.center.x
                    
                    // Draw track background
                    drawLine(
                        color = colors.trackColor,
                        start = Offset(centerX, trackWidth),
                        end = Offset(centerX, size.height - trackWidth),
                        strokeWidth = trackWidth,
                        cap = StrokeCap.Round
                    )
                    
                    // Draw active track
                    val thumbY = size.height * (1f - normalizedValue)
                    drawLine(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                colors.activeTrackColor,
                                colors.activeTrackColor.copy(alpha = 0.7f)
                            )
                        ),
                        start = Offset(centerX, thumbY),
                        end = Offset(centerX, size.height - trackWidth),
                        strokeWidth = trackWidth,
                        cap = StrokeCap.Round
                    )
                    
                    // Draw tick marks if enabled
                    if (showTicks && steps > 0) {
                        drawTicks(
                            steps = steps,
                            centerX = centerX,
                            height = size.height,
                            trackWidth = trackWidth,
                            color = colors.tickColor
                        )
                    }
                    
                    // Draw thumb
                    drawCircle(
                        color = colors.thumbColor,
                        radius = if (isDragging) trackWidth * 1.2f else trackWidth,
                        center = Offset(centerX, thumbY)
                    )
                    
                    // Draw thumb border
                    drawCircle(
                        color = colors.thumbBorderColor,
                        radius = if (isDragging) trackWidth * 1.2f else trackWidth,
                        center = Offset(centerX, thumbY),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }
    }
}

/**
 * Helper function to draw tick marks on the slider
 */
private fun DrawScope.drawTicks(
    steps: Int,
    centerX: Float,
    height: Float,
    trackWidth: Float,
    color: Color
) {
    val tickCount = steps + 1
    val tickSpacing = height / steps
    
    for (i in 0..steps) {
        val y = i * tickSpacing
        drawLine(
            color = color,
            start = Offset(centerX - trackWidth * 0.8f, y),
            end = Offset(centerX + trackWidth * 0.8f, y),
            strokeWidth = 1.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

/**
 * Helper function to update slider value with optional haptic feedback
 */
private fun updateValue(
    y: Float,
    height: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    haptics: HapticFeedback,
    enableHaptics: Boolean
) {
    val normalizedPosition = 1f - (y / height).coerceIn(0f, 1f)
    var newValue = valueRange.start + normalizedPosition * (valueRange.endInclusive - valueRange.start)
    
    // Snap to steps if defined
    if (steps > 0) {
        val stepSize = (valueRange.endInclusive - valueRange.start) / steps
        val steppedValue = ((newValue - valueRange.start) / stepSize).roundToInt() * stepSize + valueRange.start
        
        // Haptic feedback when crossing a step
        if (enableHaptics && steppedValue != newValue) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        
        newValue = steppedValue
    }
    
    onValueChange(newValue.coerceIn(valueRange.start, valueRange.endInclusive))
}

/**
 * Colors configuration for VerticalSlider
 */
data class VerticalSliderColors(
    val trackColor: Color,
    val activeTrackColor: Color,
    val thumbColor: Color,
    val thumbBorderColor: Color,
    val tickColor: Color,
    val labelBackground: Color,
    val labelText: Color
)

/**
 * Default configurations for VerticalSlider
 */
object VerticalSliderDefaults {
    @Composable
    fun colors(
        trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
        activeTrackColor: Color = MaterialTheme.colorScheme.primary,
        thumbColor: Color = MaterialTheme.colorScheme.primary,
        thumbBorderColor: Color = MaterialTheme.colorScheme.surface,
        tickColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        labelBackground: Color = MaterialTheme.colorScheme.secondaryContainer,
        labelText: Color = MaterialTheme.colorScheme.onSecondaryContainer
    ) = VerticalSliderColors(
        trackColor = trackColor,
        activeTrackColor = activeTrackColor,
        thumbColor = thumbColor,
        thumbBorderColor = thumbBorderColor,
        tickColor = tickColor,
        labelBackground = labelBackground,
        labelText = labelText
    )
}

/**
 * Preview-friendly sample usage
 */
@Composable
fun VerticalSliderSample() {
    var value by remember { mutableStateOf(50f) }
    
    VerticalSlider(
        value = value,
        onValueChange = { value = it },
        valueRange = 0f..100f,
        steps = 10,
        showTicks = true,
        labelFormatter = { "${it.roundToInt()}%" }
    )
}
