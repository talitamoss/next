// app/src/main/java/com/domain/app/ui/components/core/sliders/VerticalSlider.kt
package com.domain.app.ui.components.core.sliders

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * A highly customizable vertical slider component that can be reused across all dialogs.
 * Supports both modern filled style and classic line style.
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
 * @param width Width of the slider container
 * @param colors Custom colors for the slider
 * @param style Visual style of the slider (Modern or Classic)
 * @param trackWidth Width of the track for Classic style
 * @param hapticFeedback Whether to provide haptic feedback
 * @param labelFormatter Custom formatter for the value label
 * @param sideLabels Optional labels for top and bottom of slider
 * @param descriptionProvider Optional function to provide descriptive text based on value
 * @param gradientColors Optional gradient colors for Modern style
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
    width: Dp = 80.dp,
    colors: VerticalSliderColors = VerticalSliderDefaults.colors(),
    style: SliderStyle = SliderStyle.Modern,
    trackWidth: Dp = 6.dp,
    hapticFeedback: Boolean = true,
    labelFormatter: (Float) -> String = { it.roundToInt().toString() },
    sideLabels: Pair<String, String>? = null,
    descriptionProvider: ((Float) -> String)? = null,
    gradientColors: List<Color>? = null
) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    
    var sliderHeight by remember { mutableStateOf(0) }
    var isDragging by remember { mutableStateOf(false) }
    
    // Animated value for smooth transitions
    val animatedValue by animateFloatAsState(
        targetValue = value,
        animationSpec = spring(dampingRatio = 0.8f),
        label = "slider_value"
    )
    
    // Calculate normalized position (0 to 1)
    val normalizedValue = ((animatedValue - valueRange.start) / (valueRange.endInclusive - valueRange.start))
        .coerceIn(0f, 1f)
    
    Row(
        modifier = modifier
            .height(height)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side labels if provided
        if (sideLabels != null) {
            Column(
                modifier = Modifier.padding(end = 16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = sideLabels.first,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.labelText,
                    textAlign = TextAlign.End
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = sideLabels.second,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.labelText.copy(alpha = 0.7f),
                    textAlign = TextAlign.End
                )
            }
        }
        
        // Main slider container
        Box(
            modifier = Modifier
                .width(width)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            when (style) {
                SliderStyle.Modern -> {
                    // Modern filled style with gradient background
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(width / 2))
                            .background(
                                if (gradientColors != null) {
                                    Brush.verticalGradient(
                                        colors = gradientColors,
                                        startY = 0f,
                                        endY = Float.POSITIVE_INFINITY
                                    )
                                } else {
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            colors.trackColor.copy(alpha = 0.1f),
                                            colors.trackColor.copy(alpha = 0.3f),
                                            colors.trackColor.copy(alpha = 0.5f)
                                        )
                                    )
                                }
                            )
                    ) {
                        VerticalSliderContent(
                            value = animatedValue,
                            normalizedValue = normalizedValue,
                            onValueChange = onValueChange,
                            sliderHeight = sliderHeight,
                            isDragging = isDragging,
                            onDragStateChange = { isDragging = it },
                            valueRange = valueRange,
                            steps = steps,
                            enabled = enabled,
                            showTicks = showTicks,
                            colors = colors,
                            style = style,
                            trackWidth = with(density) { trackWidth.toPx() },
                            haptics = haptics,
                            hapticFeedback = hapticFeedback,
                            modifier = Modifier
                                .fillMaxSize()
                                .onSizeChanged { size ->
                                    sliderHeight = size.height
                                }
                        )
                    }
                }
                
                SliderStyle.Classic -> {
                    // Classic line style
                    VerticalSliderContent(
                        value = animatedValue,
                        normalizedValue = normalizedValue,
                        onValueChange = onValueChange,
                        sliderHeight = sliderHeight,
                        isDragging = isDragging,
                        onDragStateChange = { isDragging = it },
                        valueRange = valueRange,
                        steps = steps,
                        enabled = enabled,
                        showTicks = showTicks,
                        colors = colors,
                        style = style,
                        trackWidth = with(density) { trackWidth.toPx() },
                        haptics = haptics,
                        hapticFeedback = hapticFeedback,
                        modifier = Modifier
                            .fillMaxSize()
                            .onSizeChanged { size ->
                                sliderHeight = size.height
                            }
                    )
                }
            }
        }
        
        // Right side value display
        Column(
            modifier = Modifier.padding(start = 16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            if (showLabel) {
                Text(
                    text = labelFormatter(animatedValue),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.labelText
                )
            }
            
            descriptionProvider?.let { provider ->
                Text(
                    text = provider(animatedValue),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.labelText.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun VerticalSliderContent(
    value: Float,
    normalizedValue: Float,
    onValueChange: (Float) -> Unit,
    sliderHeight: Int,
    isDragging: Boolean,
    onDragStateChange: (Boolean) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    enabled: Boolean,
    showTicks: Boolean,
    colors: VerticalSliderColors,
    style: SliderStyle,
    trackWidth: Float,
    haptics: androidx.compose.ui.hapticfeedback.HapticFeedback,
    hapticFeedback: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .pointerInput(enabled, valueRange) {
                if (!enabled) return@pointerInput
                
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        onDragStateChange(true)
                        if (hapticFeedback) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        updateValue(
                            offset.y,
                            sliderHeight,
                            valueRange,
                            steps,
                            onValueChange,
                            haptics,
                            hapticFeedback
                        )
                    },
                    onDragEnd = {
                        onDragStateChange(false)
                    }
                ) { _, dragAmount ->
                    val currentY = sliderHeight * (1f - normalizedValue)
                    val newY = (currentY + dragAmount).coerceIn(0f, sliderHeight.toFloat())
                    updateValue(
                        newY,
                        sliderHeight,
                        valueRange,
                        steps,
                        onValueChange,
                        haptics,
                        hapticFeedback
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val centerX = size.center.x
            val trackHeight = size.height
            val thumbY = trackHeight * (1f - normalizedValue)
            
            when (style) {
                SliderStyle.Modern -> {
                    // For modern style, just draw the thumb
                    // The track is the background gradient
                    
                    // Outer white circle
                    drawCircle(
                        color = Color.White,
                        radius = if (isDragging) 14.dp.toPx() else 12.dp.toPx(),
                        center = Offset(centerX, thumbY)
                    )
                    
                    // Inner colored circle
                    drawCircle(
                        color = colors.thumbColor.copy(alpha = 0.9f),
                        radius = if (isDragging) 12.dp.toPx() else 10.dp.toPx(),
                        center = Offset(centerX, thumbY)
                    )
                    
                    // Subtle shadow/glow when dragging
                    if (isDragging) {
                        drawCircle(
                            color = colors.thumbColor.copy(alpha = 0.2f),
                            radius = 20.dp.toPx(),
                            center = Offset(centerX, thumbY)
                        )
                    }
                }
                
                SliderStyle.Classic -> {
                    // Draw track background
                    drawLine(
                        color = colors.trackColor.copy(alpha = 0.3f),
                        start = Offset(centerX, 20f),
                        end = Offset(centerX, trackHeight - 20f),
                        strokeWidth = trackWidth,
                        cap = StrokeCap.Round
                    )
                    
                    // Draw active track
                    drawLine(
                        color = colors.activeTrackColor.copy(alpha = 0.6f),
                        start = Offset(centerX, thumbY),
                        end = Offset(centerX, trackHeight - 20f),
                        strokeWidth = trackWidth,
                        cap = StrokeCap.Round
                    )
                    
                    // Draw ticks if enabled
                    if (showTicks && steps > 0) {
                        drawTicks(
                            steps = steps,
                            centerX = centerX,
                            height = trackHeight,
                            trackWidth = trackWidth,
                            color = colors.tickColor
                        )
                    }
                    
                    // Draw thumb - layered circles
                    drawCircle(
                        color = Color.White,
                        radius = if (isDragging) 14.dp.toPx() else 12.dp.toPx(),
                        center = Offset(centerX, thumbY)
                    )
                    drawCircle(
                        color = colors.thumbColor,
                        radius = if (isDragging) 12.dp.toPx() else 10.dp.toPx(),
                        center = Offset(centerX, thumbY)
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
    val tickSpacing = (height - 40f) / steps // Account for padding
    
    for (i in 0..steps) {
        val y = 20f + (i * tickSpacing)
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
    haptics: androidx.compose.ui.hapticfeedback.HapticFeedback,
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
 * Visual style for the slider
 */
enum class SliderStyle {
    /** Modern style with filled gradient background */
    Modern,
    /** Classic style with line track */
    Classic
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
    
    /**
     * Mood-specific colors with gradient
     */
    @Composable
    fun moodColors() = colors(
        thumbColor = MaterialTheme.colorScheme.primary,
        labelText = MaterialTheme.colorScheme.primary
    )
    
    /**
     * Sleep-specific colors
     */
    @Composable  
    fun sleepColors() = colors(
        thumbColor = MaterialTheme.colorScheme.tertiary,
        activeTrackColor = MaterialTheme.colorScheme.tertiary,
        labelText = MaterialTheme.colorScheme.tertiary
    )
    
    /**
     * Energy-specific colors
     */
    @Composable
    fun energyColors() = colors(
        thumbColor = Color(0xFFFF6B35),
        activeTrackColor = Color(0xFFFF6B35),
        labelText = Color(0xFFFF6B35)
    )
}

/**
 * Common description providers for different use cases
 */
object SliderDescriptions {
    val mood: (Float) -> String = { value ->
        when {
            value >= 80 -> "Excellent"
            value >= 60 -> "Good"
            value >= 40 -> "Neutral"
            value >= 20 -> "Low"
            else -> "Very Low"
        }
    }
    
    val energy: (Float) -> String = { value ->
        when {
            value >= 80 -> "High Energy"
            value >= 60 -> "Active"
            value >= 40 -> "Moderate"
            value >= 20 -> "Tired"
            else -> "Exhausted"
        }
    }
    
    val stress: (Float) -> String = { value ->
        when {
            value >= 80 -> "Very Stressed"
            value >= 60 -> "Stressed"
            value >= 40 -> "Moderate"
            value >= 20 -> "Calm"
            else -> "Very Calm"
        }
    }
    
    val sleep: (Float) -> String = { value ->
        when {
            value >= 80 -> "Excellent"
            value >= 60 -> "Good"
            value >= 40 -> "Fair"
            value >= 20 -> "Poor"
            else -> "Very Poor"
        }
    }
}
