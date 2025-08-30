package com.domain.app.ui.components.core.sliders

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.RangeSlider as MaterialRangeSlider

/**
 * Color configuration for RangeSlider
 * Maintains our custom API while using Material3 underneath
 */
data class RangeSliderColors(
    val thumbColor: Color = Color(0xFF667EEA),
    val activeTrackColor: Color = Color(0xFF667EEA),
    val inactiveTrackColor: Color = Color.Gray.copy(alpha = 0.3f),
    val activeTickColor: Color = Color(0xFF667EEA),
    val inactiveTickColor: Color = Color.Gray.copy(alpha = 0.5f)
)

/**
 * Default colors provider
 */
object RangeSliderDefaults {
    @Composable
    fun colors(
        thumbColor: Color = MaterialTheme.colorScheme.primary,
        activeTrackColor: Color = MaterialTheme.colorScheme.primary,
        inactiveTrackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
        activeTickColor: Color = MaterialTheme.colorScheme.primary,
        inactiveTickColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
    ) = RangeSliderColors(
        thumbColor = thumbColor,
        activeTrackColor = activeTrackColor,
        inactiveTrackColor = inactiveTrackColor,
        activeTickColor = activeTickColor,
        inactiveTickColor = inactiveTickColor
    )
}

/**
 * A wrapper around Material3's RangeSlider that maintains our custom API
 * while using the stable Compose implementation underneath
 *
 * @param startValue Current start value
 * @param endValue Current end value
 * @param onRangeChange Callback when range changes
 * @param modifier Modifier for the component
 * @param valueRange The range of values the slider can select from
 * @param steps Number of discrete steps (0 for continuous)
 * @param minRange Minimum allowed range between start and end
 * @param colors Color configuration
 * @param showLabels Whether to show value labels above the slider
 * @param showTicks Whether to show tick marks (not supported by Material3, ignored)
 * @param labelFormatter Function to format display values
 * @param hapticFeedback Whether to provide haptic feedback (handled by Material3)
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    // Convert our single values to a range for Material3
    var sliderPosition by remember(startValue, endValue) { 
        mutableStateOf(startValue..endValue) 
    }
    
    Column(modifier = modifier) {
        // Optional labels above the slider
        if (showLabels) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Start value label
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = labelFormatter?.invoke(sliderPosition.start) 
                            ?: "%.1f".format(sliderPosition.start),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                // End value label
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = labelFormatter?.invoke(sliderPosition.endInclusive) 
                            ?: "%.1f".format(sliderPosition.endInclusive),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        
        // Material3 RangeSlider
        MaterialRangeSlider(
            value = sliderPosition,
            onValueChange = { range ->
                // Enforce minimum range if specified
                val start = range.start
                val end = range.endInclusive
                
                if (minRange > 0 && (end - start) < minRange) {
                    // Maintain minimum range
                    if (start != sliderPosition.start) {
                        // Start thumb moved
                        sliderPosition = start..(start + minRange)
                    } else {
                        // End thumb moved
                        sliderPosition = (end - minRange)..end
                    }
                } else {
                    sliderPosition = range
                }
                
                onRangeChange(sliderPosition.start, sliderPosition.endInclusive)
            },
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = colors.thumbColor,
                activeTrackColor = colors.activeTrackColor,
                inactiveTrackColor = colors.inactiveTrackColor,
                activeTickColor = colors.activeTickColor,
                inactiveTickColor = colors.inactiveTickColor
            )
        )
    }
}

/**
 * Preview helper
 */
@Composable
fun RangeSliderPreview() {
    var range by remember { mutableStateOf(20f..80f) }
    
    RangeSlider(
        startValue = range.start,
        endValue = range.endInclusive,
        onRangeChange = { start, end -> 
            range = start..end 
        },
        valueRange = 0f..100f,
        steps = 0,
        minRange = 10f,
        showLabels = true,
        labelFormatter = { "%.0f%%".format(it) },
        modifier = Modifier.padding(16.dp)
    )
}
