// app/src/main/java/com/domain/app/ui/components/core/feedback/EmptyState.kt
package com.domain.app.ui.components.core.feedback

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.domain.app.ui.theme.AppIcons

/**
 * A reusable empty state component for displaying when no data is available.
 * Replaces all the duplicate empty state implementations across screens.
 * 
 * @param title Main message to display
 * @param subtitle Optional additional context
 * @param icon Optional icon to display (defaults to appropriate icon based on context)
 * @param iconEmoji Optional emoji to use instead of icon
 * @param actionLabel Optional action button label
 * @param onAction Optional action button click handler
 * @param modifier Modifier for the component
 * @param animateIn Whether to animate the component appearance
 */
@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconEmoji: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    animateIn: Boolean = true,
    type: EmptyStateType = EmptyStateType.NO_DATA
) {
    val infiniteTransition = rememberInfiniteTransition(label = "empty_state_animation")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = true,
            enter = if (animateIn) fadeIn() + scaleIn(initialScale = 0.8f) else EnterTransition.None,
            exit = fadeOut() + scaleOut(targetScale = 0.8f)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp)
            ) {
                // Icon or Emoji
                when {
                    iconEmoji != null -> {
                        Text(
                            text = iconEmoji,
                            style = MaterialTheme.typography.displayLarge,
                            modifier = Modifier
                                .scale(scale)
                                .alpha(alpha)
                                .padding(bottom = 24.dp)
                        )
                    }
                    icon != null -> {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier
                                .size(80.dp)
                                .scale(scale)
                                .alpha(alpha)
                                .padding(bottom = 24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    else -> {
                        val defaultIcon = when (type) {
                            EmptyStateType.NO_DATA -> AppIcons.Data.chart
                            EmptyStateType.NO_RESULTS -> AppIcons.Action.search
                            EmptyStateType.ERROR -> AppIcons.Status.error
                            EmptyStateType.NO_CONNECTION -> AppIcons.Status.warning
                            EmptyStateType.COMING_SOON -> AppIcons.Status.pending
                        }
                        Icon(
                            imageVector = defaultIcon,
                            contentDescription = null,
                            modifier = Modifier
                                .size(80.dp)
                                .scale(scale)
                                .alpha(alpha)
                                .padding(bottom = 24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
                
                // Title
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // Subtitle
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                }
                
                // Action Button
                if (actionLabel != null && onAction != null) {
                    Button(
                        onClick = onAction,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(actionLabel)
                    }
                }
            }
        }
    }
}

/**
 * Types of empty states for different contexts
 */
enum class EmptyStateType {
    NO_DATA,
    NO_RESULTS,
    ERROR,
    NO_CONNECTION,
    COMING_SOON
}

/**
 * Convenience composables for common empty states
 */
@Composable
fun NoDataEmptyState(
    modifier: Modifier = Modifier,
    message: String = "No data recorded yet",
    subtitle: String = "Start tracking to see your insights here",
    onAddData: (() -> Unit)? = null
) {
    EmptyState(
        title = message,
        subtitle = subtitle,
        iconEmoji = "📊",
        actionLabel = if (onAddData != null) "Add Data" else null,
        onAction = onAddData,
        modifier = modifier,
        type = EmptyStateType.NO_DATA
    )
}

@Composable
fun NoPluginsEmptyState(
    modifier: Modifier = Modifier,
    onAddPlugin: () -> Unit
) {
    EmptyState(
        title = "No plugins enabled",
        subtitle = "Enable plugins to start tracking your behavioral data",
        iconEmoji = "🧩",
        actionLabel = "Browse Plugins",
        onAction = onAddPlugin,
        modifier = modifier,
        type = EmptyStateType.NO_DATA
    )
}

@Composable
fun ComingSoonEmptyState(
    modifier: Modifier = Modifier,
    feature: String
) {
    EmptyState(
        title = "$feature Coming Soon",
        subtitle = "This feature is under development and will be available in a future update",
        iconEmoji = "🚀",
        modifier = modifier,
        type = EmptyStateType.COMING_SOON
    )
}

@Composable
fun SearchNoResultsEmptyState(
    modifier: Modifier = Modifier,
    searchQuery: String,
    onClearSearch: () -> Unit
) {
    EmptyState(
        title = "No results found",
        subtitle = "No items match \"$searchQuery\"",
        icon = AppIcons.Action.search,
        actionLabel = "Clear Search",
        onAction = onClearSearch,
        modifier = modifier,
        type = EmptyStateType.NO_RESULTS
    )
}

@Composable
fun ErrorEmptyState(
    modifier: Modifier = Modifier,
    message: String = "Something went wrong",
    onRetry: (() -> Unit)? = null
) {
    EmptyState(
        title = message,
        subtitle = "Please try again later",
        icon = AppIcons.Status.error,
        actionLabel = if (onRetry != null) "Retry" else null,
        onAction = onRetry,
        modifier = modifier,
        type = EmptyStateType.ERROR
    )
}
