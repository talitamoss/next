package com.domain.app.ui.components.core.lists

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.domain.app.ui.theme.AppIcons
import kotlinx.coroutines.delay

/**
 * A list item that can be swiped to reveal actions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableListItem(
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null,
    onArchive: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    confirmDelete: Boolean = true,
    dismissThreshold: Float = 0.5f,
    content: @Composable () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    // Swipe right - archive/edit action
                    when {
                        onArchive != null -> {
                            onArchive()
                            true
                        }
                        onEdit != null -> {
                            onEdit()
                            false // Don't dismiss, just trigger edit
                        }
                        else -> false
                    }
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    // Swipe left - delete/share action
                    when {
                        onDelete != null -> {
                            if (confirmDelete) {
                                showDeleteDialog = true
                                false // Don't dismiss yet, show dialog
                            } else {
                                onDelete()
                                true
                            }
                        }
                        onShare != null -> {
                            onShare()
                            false // Don't dismiss, just trigger share
                        }
                        else -> false
                    }
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        },
        positionalThreshold = { totalDistance ->
            totalDistance * dismissThreshold
        }
    )
    
    // Reset state after action
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
            delay(500)
            dismissState.reset()
        }
    }
    
    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        backgroundContent = {
            SwipeBackground(
                dismissState = dismissState,
                onDelete = onDelete,
                onArchive = onArchive,
                onEdit = onEdit,
                onShare = onShare
            )
        },
        content = { content() }
    )
    
    // Delete confirmation dialog
    if (showDeleteDialog && onDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
            },
            title = { Text("Delete Item?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeBackground(
    dismissState: SwipeToDismissBoxState,
    onDelete: (() -> Unit)?,
    onArchive: (() -> Unit)?,
    onEdit: (() -> Unit)?,
    onShare: (() -> Unit)?
) {
    val direction = dismissState.dismissDirection
    val isDismissed = dismissState.currentValue != SwipeToDismissBoxValue.Settled
    
    val color by animateColorAsState(
        targetValue = when (direction) {
            SwipeToDismissBoxValue.StartToEnd -> {
                when {
                    onArchive != null -> MaterialTheme.colorScheme.primary
                    onEdit != null -> MaterialTheme.colorScheme.secondary
                    else -> Color.Transparent
                }
            }
            SwipeToDismissBoxValue.EndToStart -> {
                when {
                    onDelete != null -> MaterialTheme.colorScheme.error
                    onShare != null -> MaterialTheme.colorScheme.tertiary
                    else -> Color.Transparent
                }
            }
            else -> Color.Transparent
        },
        label = "swipe_bg_color"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isDismissed) 1.2f else 1f,
        label = "swipe_icon_scale"
    )
    
    val (icon, alignment) = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> {
            when {
                onArchive != null -> AppIcons.Action.archive to Alignment.CenterStart
                onEdit != null -> AppIcons.Action.edit to Alignment.CenterStart
                else -> null to Alignment.CenterStart
            }
        }
        SwipeToDismissBoxValue.EndToStart -> {
            when {
                onDelete != null -> AppIcons.Action.delete to Alignment.CenterEnd
                onShare != null -> AppIcons.Action.share to Alignment.CenterEnd
                else -> null to Alignment.CenterEnd
            }
        }
        else -> null to Alignment.Center
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color)
            .padding(horizontal = 20.dp),
        contentAlignment = alignment
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                modifier = Modifier.scale(scale),
                tint = MaterialTheme.colorScheme.onError
            )
        }
    }
}

/**
 * Specialized swipeable list item for data points
 */
@Composable
fun SwipeableDataItem(
    title: String,
    subtitle: String? = null,
    value: String,
    icon: ImageVector? = null,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    SwipeableListItem(
        onDelete = onDelete,
        onEdit = onEdit,
        modifier = modifier
    ) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                icon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    subtitle?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * List with swipeable items
 */
@Composable
fun <T> SwipeableList(
    items: List<T>,
    onDelete: (T) -> Unit,
    onEdit: ((T) -> Unit)? = null,
    modifier: Modifier = Modifier,
    key: ((T) -> Any)? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(8.dp),
    itemContent: @Composable (T) -> Unit
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = verticalArrangement
    ) {
        items(
            items = items,
            key = key
        ) { item ->
            SwipeableListItem(
                onDelete = { onDelete(item) },
                onEdit = onEdit?.let { { it(item) } }
            ) {
                itemContent(item)
            }
        }
    }
}
