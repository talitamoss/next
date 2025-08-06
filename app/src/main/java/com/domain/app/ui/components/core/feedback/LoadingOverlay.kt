// app/src/main/java/com/domain/app/ui/components/core/feedback/LoadingOverlay.kt
package com.domain.app.ui.components.core.feedback

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

/**
 * A reusable loading overlay component for consistent loading states across the app.
 * Can be used as a full-screen overlay or inline loading indicator.
 * 
 * @param isLoading Whether to show the loading overlay
 * @param modifier Modifier for the overlay
 * @param message Optional loading message
 * @param progress Optional progress value (0f to 1f) for determinate loading
 * @param type Type of loading indicator
 * @param fullScreen Whether to show as full-screen overlay
 * @param dismissible Whether the overlay can be dismissed by tapping outside
 * @param onDismiss Optional callback when overlay is dismissed
 */
@Composable
fun LoadingOverlay(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    message: String? = null,
    progress: Float? = null,
    type: LoadingType = LoadingType.CIRCULAR,
    fullScreen: Boolean = true,
    dismissible: Boolean = false,
    onDismiss: (() -> Unit)? = null,
    content: @Composable () -> Unit = {}
) {
    Box(modifier = modifier) {
        content()
        
        AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            if (fullScreen) {
                FullScreenLoadingOverlay(
                    message = message,
                    progress = progress,
                    type = type,
                    dismissible = dismissible,
                    onDismiss = onDismiss
                )
            } else {
                InlineLoadingIndicator(
                    message = message,
                    progress = progress,
                    type = type
                )
            }
        }
    }
}

@Composable
private fun FullScreenLoadingOverlay(
    message: String?,
    progress: Float?,
    type: LoadingType,
    dismissible: Boolean,
    onDismiss: (() -> Unit)?
) {
    Dialog(
        onDismissRequest = { if (dismissible) onDismiss?.invoke() },
        properties = DialogProperties(
            dismissOnBackPress = dismissible,
            dismissOnClickOutside = dismissible,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            LoadingContent(
                message = message,
                progress = progress,
                type = type,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(32.dp)
            )
        }
    }
}

@Composable
private fun InlineLoadingIndicator(
    message: String?,
    progress: Float?,
    type: LoadingType
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        LoadingContent(
            message = message,
            progress = progress,
            type = type,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun LoadingContent(
    message: String?,
    progress: Float?,
    type: LoadingType,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (type) {
            LoadingType.CIRCULAR -> CircularLoadingIndicator(progress)
            LoadingType.LINEAR -> LinearLoadingIndicator(progress)
            LoadingType.DOTS -> DotsLoadingIndicator()
            LoadingType.PULSE -> PulseLoadingIndicator()
            LoadingType.CUSTOM -> CustomLoadingIndicator()
        }
        
        message?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        progress?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${(it * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CircularLoadingIndicator(progress: Float?) {
    if (progress != null) {
        CircularProgressIndicator(
            progress = progress,
            modifier = Modifier.size(48.dp),
            strokeWidth = 4.dp
        )
    } else {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            strokeWidth = 4.dp
        )
    }
}

@Composable
private fun LinearLoadingIndicator(progress: Float?) {
    if (progress != null) {
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .width(200.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
        )
    } else {
        LinearProgressIndicator(
            modifier = Modifier
                .width(200.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
        )
    }
}

@Composable
private fun DotsLoadingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val animationDelay = index * 150
            
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.5f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = animationDelay),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot_scale_$index"
            )
            
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .scale(scale)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(50)
                    )
            )
        }
    }
}

@Composable
private fun PulseLoadingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    
    Box(
        modifier = Modifier
            .size(60.dp)
            .scale(scale)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                        MaterialTheme.colorScheme.primary.copy(alpha = alpha * 0.3f)
                    )
                ),
                shape = RoundedCornerShape(50)
            )
    )
}

@Composable
private fun CustomLoadingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "custom")
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    Box(
        modifier = Modifier
            .size(48.dp)
            .rotate(rotation)
    ) {
        repeat(8) { index ->
            val angle = index * 45f
            val delay = index * 100
            
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, delayMillis = delay),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_scale_$index"
            )
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(angle)
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(16.dp)
                        .scale(scaleY = scale)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(2.dp)
                        )
                        .align(Alignment.TopCenter)
                )
            }
        }
    }
}

/**
 * Types of loading indicators
 */
enum class LoadingType {
    CIRCULAR,
    LINEAR,
    DOTS,
    PULSE,
    CUSTOM
}

/**
 * Composable that shows content with a loading state
 */
@Composable
fun LoadingContainer(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    loadingMessage: String? = null,
    errorMessage: String? = null,
    onRetry: (() -> Unit)? = null,
    emptyMessage: String = "No data available",
    isEmpty: Boolean = false,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            isLoading -> {
                LoadingOverlay(
                    isLoading = true,
                    message = loadingMessage,
                    fullScreen = false
                )
            }
            errorMessage != null -> {
                ErrorEmptyState(
                    message = errorMessage,
                    onRetry = onRetry
                )
            }
            isEmpty -> {
                NoDataEmptyState(
                    message = emptyMessage
                )
            }
            else -> {
                content()
            }
        }
    }
}

/**
 * Simple loading button that shows loading state
 */
@Composable
fun LoadingButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    text: String,
    loadingText: String = "Loading..."
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled && !isLoading
    ) {
        if (isLoading) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(loadingText)
            }
        } else {
            Text(text)
        }
    }
}
