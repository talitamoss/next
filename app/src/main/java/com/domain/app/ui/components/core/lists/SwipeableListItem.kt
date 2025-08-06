// app/src/main/java/com/domain/app/ui/components/core/lists/SwipeableListItem.kt
package com.domain.app.ui.components.core.lists

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
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
                                // Snap back
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
            .clip(MaterialTheme.shapes.medium),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side (swipe to end action)
        if (swipeToEndAction != null && offsetX > 0) {
            val backgroundColor by animateColorAsState(
                targetValue = if (progress > 0.3f) {
                    swipeToEndAction.activeColor
                } else {
                    swipeToEndAction.backgroundColor
                },
                label = "end_bg_color"
            )
            
            val iconScale by animateFloatAsState(
                targetValue = if (progress > 0.3f) 1.2f else 1f,
                label = "end_icon_scale"
            )
            
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .background(backgroundColor),
                contentAlignment = Alignment.CenterStart
            ) {
                Icon(
                    imageVector = swipeToEndAction.icon,
                    contentDescription = swipeToEndAction.label,
                    tint = Color.White,
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .scale(iconScale)
                )
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
        
        // Right side (swipe to start action)
        if (swipeToStartAction != null && offsetX < 0) {
            val backgroundColor by animateColorAsState(
                targetValue = if (progress > 0.3f) {
                    swipeToStartAction.activeColor
                } else {
                    swipeToStartAction.backgroundColor
                },
                label = "start_bg_color"
            )
            
            val iconScale by animateFloatAsState(
                targetValue = if (progress > 0.3f) 1.2f else 1f,
                label = "start_icon_scale"
            )
            
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .background(backgroundColor),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = swipeToStartAction.icon,
                    contentDescription = swipeToStartAction.label,
                    tint = Color.White,
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .scale(iconScale)
                )
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

/**
 * Predefined swipe actions
 */
sealed class SwipeAction(
    val icon: ImageVector,
    val label: String,
    val backgroundColor: Color,
    val activeColor: Color
) {
    object Delete : SwipeAction(
        icon = AppIcons.Action.delete,
        label = "Delete",
        backgroundColor = Color(0xFFFF6B6B),
        activeColor = Color(0xFFFF3333)
    )
    
    object Archive : SwipeAction(
        icon = AppIcons.Storage.archive,
        label = "Archive",
        backgroundColor = Color(0xFF4ECDC4),
        activeColor = Color(0xFF2A9D8F)
    )
    
    object Edit : SwipeAction(
        icon = AppIcons.Action.edit,
        label = "Edit",
        backgroundColor = Color(0xFF95E1D3),
        activeColor = Color(0xFF3FC1C9)
    )
    
    object Share : SwipeAction(
        icon = AppIcons.Action.share,
        label = "Share",
        backgroundColor = Color(0xFF6C5CE7),
        activeColor = Color(0xFF5F3DC4)
    )
    
    object Favorite : SwipeAction(
        icon = AppIcons.Action.favorite,
        label = "Favorite",
        backgroundColor = Color(0xFFFFD93D),
        activeColor = Color(0xFFFFC107)
    )
    
    data class Custom(
        val customIcon: ImageVector,
        val customLabel: String,
        val customBackgroundColor: Color,
        val customActiveColor: Color
    ) : SwipeAction(customIcon, customLabel, customBackgroundColor, customActiveColor)
}

/**
 * Simple list item with swipe to delete
 */
@Composable
fun SwipeToDeleteListItem(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    SwipeableListItem(
        modifier = modifier,
        onSwipeToStart = onDelete,
        swipeToStartAction = SwipeAction.Delete,
        enableSwipeToEnd = false,
        content = content
    )
}

/**
 * List item with both archive and delete actions
 */
@Composable
fun SwipeableDataListItem(
    onDelete: () -> Unit,
    onArchive: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    SwipeableListItem(
        modifier = modifier,
        onSwipeToStart = onDelete,
        onSwipeToEnd = onArchive,
        swipeToStartAction = SwipeAction.Delete,
        swipeToEndAction = SwipeAction.Archive,
        content = content
    )
}

// Note: Add these to AppIcons.kt if not already present:
// object Storage {
//     val folder = Icons.Filled.Folder
//     val archive = Icons.Filled.Archive
//     val cloud = Icons.Filled.Cloud
//     val save = Icons.Filled.Save
// }
// object Action {
//     val favorite = Icons.Filled.Favorite
// }
