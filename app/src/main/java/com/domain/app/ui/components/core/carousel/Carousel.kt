// app/src/main/java/com/domain/app/ui/components/core/carousel/Carousel.kt
package com.domain.app.ui.components.core.carousel

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sign

/**
 * A horizontal carousel selector component for choosing from a list of options.
 * Features smooth scrolling, center-snapping, and visual feedback.
 *
 * @param options List of options to display
 * @param selectedOption Currently selected option
 * @param onOptionSelected Callback when an option is selected
 * @param modifier Modifier for the carousel container
 * @param height Height of the carousel
 * @param itemWidth Width of each item
 * @param colors Color configuration for the carousel
 * @param hapticFeedback Whether to provide haptic feedback on selection
 * @param velocityThreshold Base velocity threshold for sensitivity (default: 1000f)
 * @param maxItemsToSkip Maximum number of items that can be skipped in one swipe (default: 5)
 * @param minVelocity Minimum velocity required to trigger any movement (default: 50f)
 */
@Composable
fun Carousel(
    options: List<CarouselOption>,
    selectedOption: CarouselOption?,
    onOptionSelected: (CarouselOption) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 80.dp,
    itemWidth: Dp = 120.dp,
    colors: CarouselColors = CarouselDefaults.colors(),
    hapticFeedback: Boolean = true,
    velocityThreshold: Float = 1000f,  // NEW: Base velocity for sensitivity
    maxItemsToSkip: Int = 5,          // NEW: Maximum items to skip in one swipe
    minVelocity: Float = 50f          // NEW: Minimum velocity to trigger movement
) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    
    var containerWidth by remember { mutableStateOf(0) }
    val itemWidthPx = with(density) { itemWidth.toPx() }
    val containerWidthDp = with(density) { containerWidth.toDp() }
    
    // Calculate padding to center items
    val sidePadding = maxOf(0.dp, (containerWidthDp - itemWidth) / 2)
    
    // Track if we're currently dragging
    var isDragging by remember { mutableStateOf(false) }
    var dragStartX by remember { mutableStateOf(0f) }
    var dragStartTime by remember { mutableStateOf(0L) }
    
    // Auto-scroll to selected item when it changes (but not while dragging)
    LaunchedEffect(selectedOption) {
        if (!isDragging) {
            selectedOption?.let { selected ->
                val index = options.indexOf(selected)
                if (index >= 0) {
                    val targetScroll = (index * itemWidthPx).toInt()
                    scrollState.animateScrollTo(targetScroll)
                }
            }
        }
    }
    
    // Create draggable state for gesture handling
    val draggableState = rememberDraggableState { delta ->
        coroutineScope.launch {
            scrollState.scrollBy(-delta)
        }
    }
    
    // Function to snap to the nearest item with velocity-based scrolling
    fun snapToNearestItem(velocity: Float = 0f) {
        coroutineScope.launch {
            val currentScroll = scrollState.value
            
            // Calculate target based on velocity and current position
            val targetIndex = if (abs(velocity) < minVelocity) {
                // Low velocity: snap to nearest item
                (currentScroll / itemWidthPx).roundToInt()
            } else {
                // Use velocity to determine how far to scroll
                // This creates a smooth, proportional response to swipe speed
                val currentExactPosition = currentScroll / itemWidthPx
                
                // Calculate distance based on velocity (smooth curve, not steps)
                // The faster the swipe, the more items we skip
                val velocityFactor = (abs(velocity) / velocityThreshold).coerceIn(0.1f, maxItemsToSkip.toFloat())
                
                // Apply a smoothing function (logarithmic feels more natural than linear)
                val smoothedFactor = if (velocityFactor > 1f) {
                    1f + kotlin.math.ln(velocityFactor)
                } else {
                    velocityFactor
                }
                
                // Calculate the target position
                val direction = -sign(velocity) // Negative velocity = forward swipe
                val targetPosition = currentExactPosition + (direction * smoothedFactor)
                
                // Round to nearest item and constrain to bounds
                targetPosition.roundToInt()
            }.coerceIn(0, options.size - 1)
            
            // Calculate target scroll position
            val targetScroll = (targetIndex * itemWidthPx).toInt()
            
            // Use a single smooth animation spec
            // Duration adjusts based on distance for consistent feel
            val distance = abs(targetScroll - currentScroll)
            val baseDuration = 300 // milliseconds
            val duration = (baseDuration + (distance / itemWidthPx * 50)).toInt().coerceIn(300, 600)
            
            val animationSpec = tween<Float>(
                durationMillis = duration,
                easing = FastOutSlowInEasing
            )
            
            // Animate to target position
            scrollState.animateScrollTo(targetScroll, animationSpec)
            
            // Provide haptic feedback
            if (hapticFeedback && targetIndex != (currentScroll / itemWidthPx).roundToInt()) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            
            // Select the item we snapped to
            options.getOrNull(targetIndex)?.let { onOptionSelected(it) }
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .onSizeChanged { containerWidth = it.width }
    ) {
        // Carousel items with improved scrolling
        Row(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(scrollState)
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Horizontal,
                    onDragStarted = { startPos ->
                        isDragging = true
                        dragStartX = startPos.x
                        dragStartTime = System.currentTimeMillis()
                    },
                    onDragStopped = { velocity ->
                        isDragging = false
                        // Use velocity for momentum-based scrolling
                        snapToNearestItem(-velocity) // Negative to match scroll direction
                    }
                ),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Leading padding
            Spacer(modifier = Modifier.width(sidePadding))
            
            // Items
            options.forEachIndexed { index, option ->
                val distanceFromCenter = abs(scrollState.value - (index * itemWidthPx))
                val scale = (1f - (distanceFromCenter / (itemWidthPx * 3f))).coerceIn(0.8f, 1f)
                
                CarouselItem(
                    option = option,
                    isSelected = option == selectedOption,
                    width = itemWidth,
                    colors = colors,
                    scale = scale,
                    onClick = {
                        if (hapticFeedback) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        onOptionSelected(option)
                        
                        // Scroll to center the selected item
                        coroutineScope.launch {
                            scrollState.animateScrollTo((index * itemWidthPx).toInt())
                        }
                    }
                )
            }
            
            // Trailing padding
            Spacer(modifier = Modifier.width(sidePadding))
        }
        
        // Gradient overlays for fade effect (optional, for visual polish)
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left fade
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                colors.backgroundColor,
                                colors.backgroundColor.copy(alpha = 0f)
                            )
                        )
                    )
            )
            
            // Right fade
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                colors.backgroundColor.copy(alpha = 0f),
                                colors.backgroundColor
                            )
                        )
                    )
            )
        }
        
        // Optional: Selection indicator (a subtle line or box around selected item area)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .width(itemWidth + 8.dp)
                .height(height - 16.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(Color.Transparent)
        )
    }
}

/**
 * Individual carousel item
 */
@Composable
private fun CarouselItem(
    option: CarouselOption,
    isSelected: Boolean,
    width: Dp,
    colors: CarouselColors,
    scale: Float = 1f,
    onClick: () -> Unit
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 1.15f else scale,
        animationSpec = spring(),
        label = "scale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.7f,
        animationSpec = spring(),
        label = "alpha"
    )
    
    Box(
        modifier = Modifier
            .width(width)
            .scale(animatedScale)
            .alpha(alpha)
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isSelected) colors.selectionIndicatorColor 
                else Color.Transparent
            )
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = option.label,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = if (isSelected) 16.sp else 14.sp
            ),
            color = if (isSelected) colors.selectedTextColor else colors.unselectedTextColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
        )
    }
}

/**
 * Data class for carousel options
 */
data class CarouselOption(
    val label: String,
    val value: Any,
    val icon: String? = null  // Made optional with default null
)

/**
 * Colors for carousel theming
 */
data class CarouselColors(
    val backgroundColor: Color,
    val selectedTextColor: Color,
    val unselectedTextColor: Color,
    val selectionIndicatorColor: Color
)

/**
 * Default carousel configurations
 */
object CarouselDefaults {
    
    @Composable
    fun colors(
        backgroundColor: Color = MaterialTheme.colorScheme.surface,
        selectedTextColor: Color = MaterialTheme.colorScheme.primary,
        unselectedTextColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        selectionIndicatorColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    ): CarouselColors = CarouselColors(
        backgroundColor = backgroundColor,
        selectedTextColor = selectedTextColor,
        unselectedTextColor = unselectedTextColor,
        selectionIndicatorColor = selectionIndicatorColor
    )
    
    @Composable
    fun movementColors(): CarouselColors = colors(
        selectedTextColor = Color(0xFF9C27B0), // Purple for movement
        selectionIndicatorColor = Color(0xFF9C27B0).copy(alpha = 0.12f)
    )
    
    @Composable
    fun energyColors(): CarouselColors = colors(
        selectedTextColor = Color(0xFFFF6B35), // Orange for energy
        selectionIndicatorColor = Color(0xFFFF6B35).copy(alpha = 0.12f)
    )
    
    @Composable
    fun moodColors(): CarouselColors = colors(
        selectedTextColor = Color(0xFF4CAF50), // Green for mood
        selectionIndicatorColor = Color(0xFF4CAF50).copy(alpha = 0.12f)
    )
}
