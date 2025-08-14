// app/src/main/java/com/domain/app/ui/components/core/sliders/RangeSlider.kt
package com.domain.app.ui.components.core.sliders

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * A dual-ended range slider component for selecting a range of values.
 * Features two draggable thumbs for start and end values with a colored range between them.
 *
 * @param startValue Current start value
 * @param endValue Current end value
 * @param onRangeChange Callback when range changes
 * @param modifier Modifier for the component
 * @param valueRange The range of values the slider can select from
 * @param steps Number of discrete steps (0 for continuous)
 * @param minRange Minimum allowed range between start and end
 * @param colors Color configuration
 * @param showLabels Whether to show value labels
 * @param showTicks Whether to show tick marks
 * @param labelFormatter Function to format display values
 * @param hapticFeedback Whether to provide haptic feedback
 */
@Composable
fun RangeSlider(
    startValue: Float,
    endValue: Float,
    onRangeChange: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..100f,
    steps: Int = 0,
    minRange: Float = 0f,
    colors: RangeSliderColors = RangeSliderDefaults.colors(),
    showLabels: Boolean = true,
    showTicks: Boolean = false,
    labelFormatter: ((Float) -> String)? = null,
    hapticFeedback: Boolean = true
) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    
    var sliderWidth by remember { mutableStateOf(0f) }
    var isDraggingStart by remember { mutableStateOf(false) }
    var isDraggingEnd by remember { mutableStateOf(false) }
    
    // Ensure values are within range and maintain minimum range
    val constrainedStart = startValue.coerceIn(valueRange.start, valueRange.endInclusive)
    val constrainedEnd = endValue.coerceIn(valueRange.start, valueRange.endInclusive)
        .coerceAtLeast(constrainedStart + minRange)
    
    // Calculate positions as percentages
    val startPercent = ((constrainedStart - valueRange.start) / 
        (valueRange.endInclusive - valueRange.start))
    val endPercent = ((constrainedEnd - valueRange.start) / 
        (valueRange.endInclusive - valueRange.start))
    
    // Animated scales for thumbs
    val startThumbScale by animateFloatAsState(
        targetValue = if (isDraggingStart) 1.1f else 1f,
        animationSpec = spring(),
        label = "startThumbScale"
    )
    
    val endThumbScale by animateFloatAsState(
        targetValue = if (isDraggingEnd) 1.1f else 1f,
        animationSpec = spring(),
        label = "endThumbScale"
    )
    
    Column(modifier = modifier.fillMaxWidth()) {
        // Value display
        if (showLabels) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Start value
                Surface(
                    color = colors.labelBackgroundColor,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = labelFormatter?.invoke(constrainedStart) 
                            ?: constrainedStart.roundToInt().toString(),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            color = colors.labelTextColor
                        )
                    )
                }
                
                // Range indicator
                Text(
                    text = "→",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.rangeIndicatorColor
                )
                
                // End value
                Surface(
                    color = colors.labelBackgroundColor,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = labelFormatter?.invoke(constrainedEnd) 
                            ?: constrainedEnd.roundToInt().toString(),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            color = colors.labelTextColor
                        )
                    )
                }
            }
        }
        
        // Slider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .onSizeChanged { sliderWidth = it.width.toFloat() }
        ) {
            // Track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(colors.trackHeight)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(colors.trackHeight / 2))
                    .background(colors.inactiveTrackColor)
            )
            
            // Active range
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .align(Alignment.CenterStart)
            ) {
                Box(
                    modifier = Modifier
                        .padding(start = (startPercent * sliderWidth / density.density).dp)
                        .width(((endPercent - startPercent) * sliderWidth / density.density).dp)
                        .height(colors.trackHeight)
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(colors.trackHeight / 2))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    colors.activeTrackStartColor,
                                    colors.activeTrackEndColor
                                )
                            )
                        )
                )
            }
            
            // Tick marks
            if (showTicks && steps > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    repeat(steps + 1) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(8.dp)
                                .background(colors.tickColor)
                        )
                    }
                }
            }
            
            // Start thumb
            RangeThumb(
                position = startPercent,
                scale = startThumbScale,
                colors = colors,
                isStart = true,
                onDrag = { delta ->
                    if (sliderWidth > 0) {
                        val deltaPercent = delta / sliderWidth
                        val newPercent = (startPercent + deltaPercent).coerceIn(0f, 1f)
                        var newValue = valueRange.start + 
                            (newPercent * (valueRange.endInclusive - valueRange.start))
                        
                        // Apply steps if specified
                        if (steps > 0) {
                            val stepSize = (valueRange.endInclusive - valueRange.start) / steps
                            newValue = (newValue / stepSize).roundToInt() * stepSize
                        }
                        
                        // Ensure minimum range
                        newValue = newValue.coerceAtMost(constrainedEnd - minRange)
                        
                        if (newValue != constrainedStart) {
                            if (hapticFeedback && steps > 0) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            onRangeChange(newValue, constrainedEnd)
                        }
                    }
                },
                onDragStart = { isDraggingStart = true },
                onDragEnd = { isDraggingStart = false }
            )
            
            // End thumb
            RangeThumb(
                position = endPercent,
                scale = endThumbScale,
                colors = colors,
                isStart = false,
                onDrag = { delta ->
                    if (sliderWidth > 0) {
                        val deltaPercent = delta / sliderWidth
                        val newPercent = (endPercent + deltaPercent).coerceIn(0f, 1f)
                        var newValue = valueRange.start + 
                            (newPercent * (valueRange.endInclusive - valueRange.start))
                        
                        // Apply steps if specified
                        if (steps > 0) {
                            val stepSize = (valueRange.endInclusive - valueRange.start) / steps
                            newValue = (newValue / stepSize).roundToInt() * stepSize
                        }
                        
                        // Ensure minimum range
                        newValue = newValue.coerceAtLeast(constrainedStart + minRange)
                        
                        if (newValue != constrainedEnd) {
                            if (hapticFeedback && steps > 0) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            onRangeChange(constrainedStart, newValue)
                        }
                    }
                },
                onDragStart = { isDraggingEnd = true },
                onDragEnd = { isDraggingEnd = false }
            )
        }
        
        // Scale marks
        if (showTicks && steps > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                repeat(steps + 1) { index ->
                    val value = valueRange.start + 
                        (index * (valueRange.endInclusive - valueRange.start) / steps)
                    Text(
                        text = labelFormatter?.invoke(value) ?: value.roundToInt().toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.tickLabelColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Individual thumb component for the range slider
 */
@Composable
private fun RangeThumb(
    position: Float,
    scale: Float,
    colors: RangeSliderColors,
    isStart: Boolean,
    onDrag: (Float) -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .offset(x = (position * 100).dp)
                .size(colors.thumbSize)
                .scale(scale)
                .align(Alignment.CenterStart)
                .shadow(
                    elevation = if (scale > 1f) 8.dp else 4.dp,
                    shape = CircleShape
                )
                .clip(CircleShape)
                .background(colors.thumbColor)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { onDragStart() },
                        onDragEnd = { onDragEnd() },
                        onHorizontalDrag = { _, dragAmount ->
                            onDrag(dragAmount)
                        }
                    )
                }
        ) {
            // Inner circle for visual depth
            Box(
                modifier = Modifier
                    .size(colors.thumbSize * 0.4f)
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(
                        if (isStart) colors.startThumbAccentColor 
                        else colors.endThumbAccentColor
                    )
            )
        }
    }
}

/**
 * Colors for the range slider
 */
data class RangeSliderColors(
    val activeTrackStartColor: Color,
    val activeTrackEndColor: Color,
    val inactiveTrackColor: Color,
    val thumbColor: Color,
    val startThumbAccentColor: Color,
    val endThumbAccentColor: Color,
    val tickColor: Color,
    val tickLabelColor: Color,
    val labelBackgroundColor: Color,
    val labelTextColor: Color,
    val rangeIndicatorColor: Color,
    val trackHeight: Dp,
    val thumbSize: Dp
)

/**
 * Default configurations for RangeSlider
 */
object RangeSliderDefaults {
    
    @Composable
    fun colors(
        activeTrackStartColor: Color = Color(0xFF667EEA),
        activeTrackEndColor: Color = Color(0xFF764BA2),
        inactiveTrackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
        thumbColor: Color = MaterialTheme.colorScheme.surface,
        startThumbAccentColor: Color = Color(0xFF667EEA),
        endThumbAccentColor: Color = Color(0xFF764BA2),
        tickColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        tickLabelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        labelBackgroundColor: Color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        labelTextColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
        rangeIndicatorColor: Color = MaterialTheme.colorScheme.primary,
        trackHeight: Dp = 6.dp,
        thumbSize: Dp = 24.dp
    ): RangeSliderColors = RangeSliderColors(
        activeTrackStartColor = activeTrackStartColor,
        activeTrackEndColor = activeTrackEndColor,
        inactiveTrackColor = inactiveTrackColor,
        thumbColor = thumbColor,
        startThumbAccentColor = startThumbAccentColor,
        endThumbAccentColor = endThumbAccentColor,
        tickColor = tickColor,
        tickLabelColor = tickLabelColor,
        labelBackgroundColor = labelBackgroundColor,
        labelTextColor = labelTextColor,
        rangeIndicatorColor = rangeIndicatorColor,
        trackHeight = trackHeight,
        thumbSize = thumbSize
    )
    
    @Composable
    fun successColors(): RangeSliderColors = colors(
        activeTrackStartColor = Color(0xFF10B981),
        activeTrackEndColor = Color(0xFF059669),
        startThumbAccentColor = Color(0xFF10B981),
        endThumbAccentColor = Color(0xFF059669)
    )
    
    @Composable
    fun warningColors(): RangeSliderColors = colors(
        activeTrackStartColor = Color(0xFFF59E0B),
        activeTrackEndColor = Color(0xFFD97706),
        startThumbAccentColor = Color(0xFFF59E0B),
        endThumbAccentColor = Color(0xFFD97706)
    )
    
    @Composable
    fun errorColors(): RangeSliderColors = colors(
        activeTrackStartColor = Color(0xFFEF4444),
        activeTrackEndColor = Color(0xFFDC2626),
        startThumbAccentColor = Color(0xFFEF4444),
        endThumbAccentColor = Color(0xFFDC2626)
    )
}
