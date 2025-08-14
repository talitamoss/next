// app/src/main/java/com/domain/app/ui/components/core/carousel/Carousel.kt
package com.domain.app.ui.components.core.carousel

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

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
    hapticFeedback: Boolean = true
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
    
    // Auto-scroll to selected item
    LaunchedEffect(selectedOption) {
        selectedOption?.let { selected ->
            val index = options.indexOf(selected)
            if (index >= 0) {
                val targetScroll = (index * itemWidthPx).toInt()
                scrollState.animateScrollTo(targetScroll)
            }
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .onSizeChanged { containerWidth = it.width }
    ) {
        // Gradient overlays for fade effect
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(60.dp)
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
        
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(60.dp)
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
        
        // Carousel items
        Row(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(scrollState)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            // Snap to nearest item
                            val currentScroll = scrollState.value.toFloat()
                            val nearestIndex = (currentScroll / itemWidthPx + 0.5f).toInt()
                                .coerceIn(0, options.size - 1)
                            
                            coroutineScope.launch {
                                scrollState.animateScrollTo((nearestIndex * itemWidthPx).toInt())
                                if (hapticFeedback && options[nearestIndex] != selectedOption) {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                onOptionSelected(options[nearestIndex])
                            }
                        }
                    ) { _, _ -> }
                },
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Leading padding
            Spacer(modifier = Modifier.width(sidePadding))
            
            // Items
            options.forEach { option ->
                CarouselItem(
                    option = option,
                    isSelected = option == selectedOption,
                    width = itemWidth,
                    colors = colors,
                    onClick = {
                        if (hapticFeedback) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        onOptionSelected(option)
                        
                        // Scroll to center the selected item
                        coroutineScope.launch {
                            val index = options.indexOf(option)
                            scrollState.animateScrollTo((index * itemWidthPx).toInt())
                        }
                    }
                )
            }
            
            // Trailing padding
            Spacer(modifier = Modifier.width(sidePadding))
        }
        
        // Selection indicator
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .width(itemWidth + 8.dp)
                .height(52.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(Color.Transparent)
                .then(
                    Modifier.background(
                        color = colors.selectionIndicatorColor,
                        shape = RoundedCornerShape(26.dp)
                    )
                )
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
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = spring(),
        label = "scale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.6f,
        animationSpec = spring(),
        label = "alpha"
    )
    
    Box(
        modifier = Modifier
            .width(width)
            .scale(scale)
            .alpha(alpha)
            .clip(RoundedCornerShape(20.dp))
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
    val icon: String? = null
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
        selectionIndicatorColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    ): CarouselColors = CarouselColors(
        backgroundColor = backgroundColor,
        selectedTextColor = selectedTextColor,
        unselectedTextColor = unselectedTextColor,
        selectionIndicatorColor = selectionIndicatorColor
    )
    
    @Composable
    fun exerciseColors(): CarouselColors = colors(
        selectedTextColor = Color(0xFF764BA2),
        selectionIndicatorColor = Color(0xFF764BA2).copy(alpha = 0.08f)
    )
}
