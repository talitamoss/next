// app/src/main/java/com/domain/app/ui/components/core/sliders/HorizontalSlider.kt
package com.domain.app.ui.components.core.sliders

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * A customizable horizontal slider component for consistent UI across the app.
 * This replaces all horizontal slider implementations in quick-add dialogs.
 * 
 * @param value Current value of the slider
 * @param onValueChange Callback when value changes
 * @param modifier Modifier for the slider container
 * @param valueRange Range of values (default 0f..100f)
 * @param steps Number of discrete steps (0 for continuous)
 * @param enabled Whether the slider is interactive
 * @param showLabel Whether to show the current value label
 * @param showTicks Whether to show tick marks for steps
 * @param showValueMarkers Whether to show value markers below the slider
 * @param height Height of the slider track
 * @param colors Custom colors for the slider
 * @param hapticFeedback Whether to provide haptic feedback
 * @param labelFormatter Custom formatter for the value label
 * @param markerLabels Optional labels for specific values
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
    colors: HorizontalSliderColors = HorizontalSliderDefaults.colors(),
    hapticFeedback: Boolean = true,
    labelFormatter: (Float) -> String = { it.roundToInt().toString() },
    markerLabels: Map<Float, String>? = null
) {
    val haptics = LocalHapticFeedback.current
    
    var sliderWidth by remember { mutableStateOf(0) }
    var isDragging by remember { mutableStateOf(false) }
    
    // Animated value for smooth transitions
    val animatedValue by animateFloatAsState(
        targetValue = value,
        label = "slider_value"
    )
    
    // Calculate normalized position (0 to 1)
    val normalizedValue = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start))
        .coerceIn(0f, 1f)
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Value label above slider
        if (showLabel) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                // Calculate label position
                val labelOffset = normalizedValue
                
                Card(
                    modifier = Modifier
                        .alpha(if (isDragging) 1f else 0.9f)
                        .fillMaxWidth(0.2f)
                        .offset(x = (labelOffset * 0.8f - 0.4f).let { (it * 100).dp }),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colors.labelBackground
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (isDragging) 6.dp else 2.dp
                    )
                ) {
                    Text(
                        text = labelFormatter(value),
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .fillMaxWidth(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.labelText,
                        textAlign = TextAlign.Center
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
                .pointerInput(enabled, valueRange) {
                    if (!enabled) return@pointerInput
                    
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            if (hapticFeedback) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            updateValue(offset.x, sliderWidth, valueRange, steps, onValueChange, haptics, hapticFeedback)
                        },
                        onDragEnd = {
                            isDragging = false
                        }
                    ) { _, dragAmount ->
                        val currentX = sliderWidth * normalizedValue
                        val newX = (currentX + dragAmount).coerceIn(0f, sliderWidth.toFloat())
                        updateValue(newX, sliderWidth, valueRange, steps, onValueChange, haptics, hapticFeedback)
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val trackHeight = height.toPx() * 0.15f
                val centerY = size.center.y
                val startPadding = trackHeight
                val endPadding = size.width - trackHeight
                val trackLength = endPadding - startPadding
                
                // Draw track background
                drawLine(
                    color = colors.trackColor,
                    start = Offset(startPadding, centerY),
                    end = Offset(endPadding, centerY),
                    strokeWidth = trackHeight,
                    cap = StrokeCap.Round
                )
                
                // Draw active track
                val thumbX = startPadding + (trackLength * normalizedValue)
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            colors.activeTrackColor.copy(alpha = 0.7f),
                            colors.activeTrackColor
                        )
                    ),
                    start = Offset(startPadding, centerY),
                    end = Offset(thumbX, centerY),
                    strokeWidth = trackHeight,
                    cap = StrokeCap.Round
                )
                
                // Draw tick marks if enabled
                if (showTicks && steps > 0) {
                    drawTicks(
                        steps = steps,
                        startX = startPadding,
                        endX = endPadding,
                        centerY = centerY,
                        trackHeight = trackHeight,
                        color = colors.tickColor
                    )
                }
                
                // Draw thumb shadow
                if (isDragging) {
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.2f),
                        radius = trackHeight * 1.8f,
                        center = Offset(thumbX, centerY + 2.dp.toPx())
                    )
                }
                
                // Draw thumb
                drawCircle(
                    color = colors.thumbColor,
                    radius = if (isDragging) trackHeight * 1.5f else trackHeight * 1.2f,
                    center = Offset(thumbX, centerY)
                )
                
                // Draw thumb border
                drawCircle(
                    color = colors.thumbBorderColor,
                    radius = if (isDragging) trackHeight * 1.5f else trackHeight * 1.2f,
                    center = Offset(thumbX, centerY),
                    style = Stroke(width = 2.dp.toPx())
                )
                
                // Draw inner thumb circle for better visual
                drawCircle(
                    color = colors.thumbBorderColor.copy(alpha = 0.3f),
                    radius = trackHeight * 0.5f,
                    center = Offset(thumbX, centerY)
                )
            }
        }
        
        // Value markers below slider
        if (showValueMarkers && markerLabels != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                markerLabels.forEach { (markerValue, label) ->
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        color = colors.markerTextColor,
                        modifier = Modifier.weight(1f),
                        textAlign = when {
                            markerValue == valueRange.start -> TextAlign.Start
                            markerValue == valueRange.endInclusive -> TextAlign.End
                            else -> TextAlign.Center
                        }
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
    startX: Float,
    endX: Float,
    centerY: Float,
    trackHeight: Float,
    color: Color
) {
    val tickCount = steps + 1
    val tickSpacing = (endX - startX) / steps
    
    for (i in 0..steps) {
        val x = startX + (i * tickSpacing)
        drawLine(
            color = color,
            start = Offset(x, centerY - trackHeight * 1.5f),
            end = Offset(x, centerY + trackHeight * 1.5f),
            strokeWidth = 1.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

/**
 * Helper function to update slider value with optional haptic feedback
 */
private fun updateValue(
    x: Float,
    width: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    haptics: HapticFeedback,
    enableHaptics: Boolean
) {
    val normalizedPosition = (x / width).coerceIn(0f, 1f)
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
 * Colors configuration for HorizontalSlider
 */
data class HorizontalSliderColors(
    val trackColor: Color,
    val activeTrackColor: Color,
    val thumbColor: Color,
    val thumbBorderColor: Color,
    val tickColor: Color,
    val labelBackground: Color,
    val labelText: Color,
    val markerTextColor: Color
)

/**
 * Default configurations for HorizontalSlider
 */
object HorizontalSliderDefaults {
    @Composable
    fun colors(
        trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
        activeTrackColor: Color = MaterialTheme.colorScheme.primary,
        thumbColor: Color = MaterialTheme.colorScheme.primary,
        thumbBorderColor: Color = MaterialTheme.colorScheme.surface,
        tickColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        labelBackground: Color = MaterialTheme.colorScheme.secondaryContainer,
        labelText: Color = MaterialTheme.colorScheme.onSecondaryContainer,
        markerTextColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
    ) = HorizontalSliderColors(
        trackColor = trackColor,
        activeTrackColor = activeTrackColor,
        thumbColor = thumbColor,
        thumbBorderColor = thumbBorderColor,
        tickColor = tickColor,
        labelBackground = labelBackground,
        labelText = labelText,
        markerTextColor = markerTextColor
    )
}

/**
 * Common preset configurations
 */
object HorizontalSliderPresets {
    /**
     * Preset for water intake (0-1000ml)
     */
    @Composable
    fun waterIntakeSlider(
        value: Float,
        onValueChange: (Float) -> Unit,
        modifier: Modifier = Modifier
    ) {
        HorizontalSlider(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            valueRange = 0f..1000f,
            steps = 10,
            showValueMarkers = true,
            labelFormatter = { "${it.roundToInt()} ml" },
            markerLabels = mapOf(
                0f to "0",
                250f to "250ml",
                500f to "500ml",
                750f to "750ml",
                1000f to "1L"
            )
        )
    }
    
    /**
     * Preset for mood rating (1-5)
     */
    @Composable
    fun moodRatingSlider(
        value: Float,
        onValueChange: (Float) -> Unit,
        modifier: Modifier = Modifier
    ) {
        HorizontalSlider(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            valueRange = 1f..5f,
            steps = 4,
            showTicks = true,
            labelFormatter = { 
                when(it.roundToInt()) {
                    1 -> "😢 Very Bad"
                    2 -> "😕 Bad"
                    3 -> "😐 Neutral"
                    4 -> "🙂 Good"
                    5 -> "😄 Excellent"
                    else -> it.roundToInt().toString()
                }
            }
        )
    }
    
    /**
     * Preset for percentage (0-100%)
     */
    @Composable
    fun percentageSlider(
        value: Float,
        onValueChange: (Float) -> Unit,
        modifier: Modifier = Modifier,
        steps: Int = 10
    ) {
        HorizontalSlider(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            valueRange = 0f..100f,
            steps = steps,
            showTicks = steps > 0,
            labelFormatter = { "${it.roundToInt()}%" }
        )
    }
}
