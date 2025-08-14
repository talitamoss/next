// app/src/main/java/com/domain/app/ui/components/core/sliders/VerticalSlider.kt
package com.domain.app.ui.components.core.sliders

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Slider visual style
 */
enum class SliderStyle {
    Modern,  // Filled gradient background
    Classic  // Thin track with fill
}

/**
 * Colors configuration for VerticalSlider
 */
data class VerticalSliderColors(
    val thumbColor: Color = Color(0xFF667EEA),
    val thumbBorderColor: Color = Color.White,
    val trackColor: Color = Color.Gray.copy(alpha = 0.3f),
    val activeTrackColor: Color = Color(0xFF667EEA),
    val labelText: Color = Color.White,
    val tickColor: Color = Color.Gray.copy(alpha = 0.5f),
    val gradientColors: List<Color>? = null
)

/**
 * Default colors provider
 */
object VerticalSliderDefaults {
    @Composable
    fun colors(
        thumbColor: Color = Color(0xFF667EEA),
        thumbBorderColor: Color = Color.White,
        trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
        activeTrackColor: Color = MaterialTheme.colorScheme.primary,
        labelText: Color = MaterialTheme.colorScheme.onSurface,
        tickColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        gradientColors: List<Color>? = listOf(Color(0xFF667EEA), Color(0xFF764BA2))
    ) = VerticalSliderColors(
        thumbColor = thumbColor,
        thumbBorderColor = thumbBorderColor,
        trackColor = trackColor,
        activeTrackColor = activeTrackColor,
        labelText = labelText,
        tickColor = tickColor,
        gradientColors = gradientColors
    )
}

/**
 * A highly customizable vertical slider component
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
    thumbSize: Dp = 24.dp,
    hapticFeedback: Boolean = true,
    labelFormatter: (Float) -> String = { it.roundToInt().toString() },
    sideLabels: Pair<String, String>? = null,
    descriptionProvider: ((Float) -> String)? = null,
    onValueChangeFinished: (() -> Unit)? = null
) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    
    var sliderHeight by remember { mutableStateOf(0) }
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
    
    Row(
        modifier = modifier.height(height),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side labels if provided
        sideLabels?.let { (topLabel, bottomLabel) ->
            Column(
                modifier = Modifier.padding(end = 16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = topLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.labelText,
                    textAlign = TextAlign.End
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = bottomLabel,
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
                    ModernVerticalSlider(
                        normalizedValue = normalizedValue,
                        onValueChange = { newNormalized ->
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
                        },
                        isDragging = isDragging,
                        onDragStateChange = { dragging ->
                            if (isDragging && !dragging) {
                                onValueChangeFinished?.invoke()
                            }
                            isDragging = dragging
                            if (dragging && hapticFeedback) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        },
                        enabled = enabled,
                        showTicks = showTicks,
                        steps = steps,
                        colors = colors,
                        thumbSize = with(density) { thumbSize.toPx() },
                        modifier = Modifier
                            .fillMaxSize()
                            .onSizeChanged { size ->
                                sliderHeight = size.height
                            }
                    )
                }
                
                SliderStyle.Classic -> {
                    ClassicVerticalSlider(
                        normalizedValue = normalizedValue,
                        onValueChange = { newNormalized ->
                            val newValue = valueRange.start + 
                                (newNormalized * (valueRange.endInclusive - valueRange.start))
                            val snappedValue = snapToStep(newValue, valueRange, steps)
                            
                            if (hapticFeedback && steps > 0) {
                                val stepSize = (valueRange.endInclusive - valueRange.start) / (steps + 1)
                                if (abs(snappedValue - lastHapticValue) >= stepSize * 0.9f) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    lastHapticValue = snappedValue
                                }
                            }
                            
                            onValueChange(snappedValue)
                        },
                        isDragging = isDragging,
                        onDragStateChange = { dragging ->
                            if (isDragging && !dragging) {
                                onValueChangeFinished?.invoke()
                            }
                            isDragging = dragging
                            if (dragging && hapticFeedback) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        },
                        enabled = enabled,
                        showTicks = showTicks,
                        steps = steps,
                        colors = colors,
                        trackWidth = with(density) { trackWidth.toPx() },
                        thumbSize = with(density) { thumbSize.toPx() },
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
        if (showLabel) {
            Column(
                modifier = Modifier.padding(start = 16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = labelFormatter(animatedValue),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.labelText
                )
                
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
}

@Composable
private fun ModernVerticalSlider(
    normalizedValue: Float,
    onValueChange: (Float) -> Unit,
    isDragging: Boolean,
    onDragStateChange: (Boolean) -> Unit,
    enabled: Boolean,
    showTicks: Boolean,
    steps: Int,
    colors: VerticalSliderColors,
    thumbSize: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (colors.gradientColors != null) {
                    Brush.verticalGradient(colors.gradientColors.reversed())
                } else {
                    Brush.verticalGradient(
                        listOf(
                            colors.activeTrackColor.copy(alpha = 0.3f),
                            colors.activeTrackColor
                        )
                    )
                }
            )
            .shadow(
                elevation = if (isDragging) 8.dp else 4.dp,
                shape = RoundedCornerShape(50),
                clip = false
            )
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        onDragStateChange(true)
                        val newNormalized = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                        onValueChange(newNormalized)
                    },
                    onDragEnd = {
                        onDragStateChange(false)
                    }
                ) { _, dragAmount ->
                    val currentY = size.height * (1f - normalizedValue)
                    val newY = (currentY - dragAmount).coerceIn(0f, size.height.toFloat())
                    val newNormalized = 1f - (newY / size.height)
                    onValueChange(newNormalized)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.center.x
            val thumbY = size.height * (1f - normalizedValue)
            
            // Draw ticks if enabled
            if (showTicks && steps > 0) {
                val stepHeight = size.height / (steps + 1)
                for (i in 0..steps + 1) {
                    val y = size.height - (i * stepHeight)
                    drawLine(
                        color = colors.tickColor,
                        start = Offset(centerX - 10.dp.toPx(), y),
                        end = Offset(centerX + 10.dp.toPx(), y),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
            
            // Draw thumb
            drawCircle(
                color = colors.thumbBorderColor,
                radius = if (isDragging) thumbSize / 2 + 2 else thumbSize / 2,
                center = Offset(centerX, thumbY)
            )
            
            drawCircle(
                color = colors.thumbColor.copy(alpha = 0.9f),
                radius = if (isDragging) thumbSize / 2 - 2 else thumbSize / 2 - 4,
                center = Offset(centerX, thumbY)
            )
        }
    }
}

@Composable
private fun ClassicVerticalSlider(
    normalizedValue: Float,
    onValueChange: (Float) -> Unit,
    isDragging: Boolean,
    onDragStateChange: (Boolean) -> Unit,
    enabled: Boolean,
    showTicks: Boolean,
    steps: Int,
    colors: VerticalSliderColors,
    trackWidth: Float,
    thumbSize: Float,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        onDragStateChange(true)
                        val newNormalized = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                        onValueChange(newNormalized)
                    },
                    onDragEnd = {
                        onDragStateChange(false)
                    }
                ) { _, dragAmount ->
                    val currentY = size.height * (1f - normalizedValue)
                    val newY = (currentY - dragAmount).coerceIn(0f, size.height.toFloat())
                    val newNormalized = 1f - (newY / size.height)
                    onValueChange(newNormalized)
                }
            }
    ) {
        val centerX = size.center.x
        val thumbY = size.height * (1f - normalizedValue)
        val padding = thumbSize / 2
        
        // Draw track
        drawLine(
            color = colors.trackColor,
            start = Offset(centerX, padding),
            end = Offset(centerX, size.height - padding),
            strokeWidth = trackWidth,
            cap = StrokeCap.Round
        )
        
        // Draw active track
        drawLine(
            color = if (colors.gradientColors != null) {
                colors.gradientColors.first()
            } else {
                colors.activeTrackColor
            },
            start = Offset(centerX, thumbY),
            end = Offset(centerX, size.height - padding),
            strokeWidth = trackWidth,
            cap = StrokeCap.Round
        )
        
        // Draw ticks if enabled
        if (showTicks && steps > 0) {
            val stepHeight = (size.height - 2 * padding) / (steps + 1)
            for (i in 0..steps + 1) {
                val y = size.height - padding - (i * stepHeight)
                drawCircle(
                    color = colors.tickColor,
                    radius = 2.dp.toPx(),
                    center = Offset(centerX, y)
                )
            }
        }
        
        // Draw thumb shadow
        drawCircle(
            color = Color.Black.copy(alpha = 0.2f),
            radius = thumbSize / 2 + 2,
            center = Offset(centerX + 2, thumbY + 2)
        )
        
        // Draw thumb
        drawCircle(
            color = colors.thumbBorderColor,
            radius = if (isDragging) thumbSize / 2 + 2 else thumbSize / 2,
            center = Offset(centerX, thumbY)
        )
        
        drawCircle(
            color = colors.thumbColor,
            radius = if (isDragging) thumbSize / 2 - 2 else thumbSize / 2 - 4,
            center = Offset(centerX, thumbY)
        )
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
