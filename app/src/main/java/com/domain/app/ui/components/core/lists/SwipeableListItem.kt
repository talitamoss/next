// app/src/main/java/com/domain/app/ui/components/core/lists/SwipeableListItem.kt
package com.domain.app.ui.components.core.lists

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.domain.app.ui.theme.AppIcons
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

/**
 * A swipeable list item with customizable swipe actions.
 * Replaces duplicate swipe implementations across screens.
 * 
 * @param modifier Modifier for the item
 * @param onSwipeToStart Action when swiping left (e.g., delete)
 * @param onSwipeToEnd Action when swiping right (e.g., archive)
 * @param swipeToStartAction Configuration for left swipe action
 * @param swipeToEndAction Configuration for right swipe action
 * @param swipeThreshold Threshold for triggering swipe action (0-1)
 * @param enableSwipeToStart Enable left swipe
 * @param enableSwipeToEnd Enable right swipe
 * @param content The main content of the list item
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableListItem(
    modifier: Modifier = Modifier,
    onSwipeToStart: (() -> Unit)? = null,
    onSwipeToEnd: (() -> Unit)? = null,
    swipeToStartAction: SwipeAction = SwipeAction.Delete,
    swipeToEndAction: SwipeAction = SwipeAction.Archive,
    swipeThreshold: Float = 0.3f,
    enableSwipeToStart: Boolean = true,
    enableSwipeToEnd: Boolean = true,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    
    var offsetX by remember { mutableFloatStateOf(0f) }
    var isDismissed by remember { mutableStateOf(false) }
    
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth()
    ) {
        val maxSwipeDistance = with(density) { constraints.maxWidth.toFloat() }
        val swipeThresholdPx = maxSwipeDistance * swipeThreshold
        
        // Background with swipe actions
        SwipeBackground(
            offsetX = offsetX,
            maxSwipeDistance = maxSwipeDistance,
            swipeToStartAction = if (enableSwipeToStart) swipeToStartAction else null,
            swipeToEndAction = if (enableSwipeToEnd) swipeToEndAction else null
        )
        
        // Main content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        val newOffset = offsetX + delta
                        offsetX = when {
                            !enableSwipeToEnd && newOffset > 0 -> 0f
                            !enableSwipeToStart && newOffset < 0 -> 0f
                            else -> newOffset.coerceIn(-maxSwipeDistance, maxSwipeDistance)
                        }
                        
                        // Haptic feedback at threshold
                        if (offsetX.absoluteValue >= swipeThresholdPx && 
                            (offsetX - delta).absoluteValue < swipeThresholdPx) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    },
                    onDragStopped = {
                        when {
                            offsetX < -swipeThresholdPx && enableSwipeToStart -> {
                                // Swipe to start (left)
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                isDismissed = true
                                offsetX = -maxSwipeDistance
                                onSwipeToStart?.invoke()
                                // Reset after action
                                scope.launch {
                                    kotlinx.coroutines.delay(300)
                                    offsetX = 0f
                                    isDismissed = false
                                }
                            }
                            offsetX > swipeThresholdPx && enableSwipeToEnd -> {
                                // Swipe to end (right)
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                isDismissed = true
                                offsetX = maxSwipeDistance
                                onSwipeToEnd?.invoke()
                                // Reset after action
                                scope.launch {
                                    kotlinx.coroutines.delay(300)
                                    offsetX = 0f
                                    isDismissed = false
                                }
                            }
                            else -> {
                                // Spring back to center
                                offsetX = 0f
                            }
                        }
                    }
                )
        ) {
            content()
        }
    }
}

@Composable
private fun SwipeBackground(
    offsetX: Float,
    maxSwipeDistance: Float,
    swipeToStartAction: SwipeAction?,
    swipeToEndAction: SwipeAction?
) {
    val progress = (offsetX.absoluteValue / maxSwipeDistance).coerceIn(0f, 1f)
    
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(
                when {
                    offsetX < 0 && swipeToStartAction != null -> 
                        swipeToStartAction.backgroundColor.copy(alpha = progress)
                    offsetX > 0 && swipeToEndAction != null -> 
                        swipeToEndAction.backgroundColor.copy(alpha = progress)
                    else -> Color.Transparent
                }
            ),
        horizontalArrangement = when {
            offsetX < 0 -> Arrangement.End
            offsetX > 0 -> Arrangement.Start
            else -> Arrangement.Center
        },
        verticalAlignment = Alignment.CenterVertically
    ) {
        val iconScale by animateFloatAsState(
            targetValue = if (progress > 0.5f) 1.2f else 1f,
            label = "icon_scale"
        )
        
        when {
            offsetX < 0 && swipeToStartAction != null -> {
                Icon(
                    imageVector = swipeToStartAction.icon,
                    contentDescription = swipeToStartAction.label,
                    tint = swipeToStartAction.iconColor,
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .scale(iconScale)
                )
            }
            offsetX > 0 && swipeToEndAction != null -> {
                Icon(
                    imageVector = swipeToEndAction.icon,
                    contentDescription = swipeToEndAction.label,
                    tint = swipeToEndAction.iconColor,
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .scale(iconScale)
                )
            }
        }
    }
}

/**
 * Configuration for swipe actions
 */
sealed class SwipeAction(
    val icon: ImageVector,
    val label: String,
    val backgroundColor: Color,
    val iconColor: Color
) {
    object Delete : SwipeAction(
        icon = Icons.Default.Delete,
        label = "Delete",
        backgroundColor = Color(0xFFFF5252),
        iconColor = Color.White
    )
    
    object Archive : SwipeAction(
        icon = AppIcons.Action.archive,
        label = "Archive",
        backgroundColor = Color(0xFF9E9E9E),
        iconColor = Color.White
    )
    
    object Save : SwipeAction(
        icon = AppIcons.Action.save,
        label = "Save",
        backgroundColor = Color(0xFF4CAF50),
        iconColor = Color.White
    )
    
    object Share : SwipeAction(
        icon = AppIcons.Action.share,
        label = "Share",
        backgroundColor = Color(0xFF2196F3),
        iconColor = Color.White
    )
    
    class Custom(
        icon: ImageVector,
        label: String,
        backgroundColor: Color = Color(0xFF607D8B),
        iconColor: Color = Color.White
    ) : SwipeAction(icon, label, backgroundColor, iconColor)
}
