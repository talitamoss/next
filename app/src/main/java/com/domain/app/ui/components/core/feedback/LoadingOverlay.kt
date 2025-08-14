package com.domain.app.ui.components.core.feedback

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

/**
 * Full-screen loading overlay with customizable message and appearance
 */
@Composable
fun LoadingOverlay(
    isLoading: Boolean,
    message: String = "Loading...",
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Black.copy(alpha = 0.5f),
    showProgress: Boolean = true,
    dismissOnBackPress: Boolean = false,
    dismissOnClickOutside: Boolean = false,
    content: @Composable () -> Unit = {}
) {
    Box(modifier = modifier) {
        content()
        
        AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                LoadingContent(
                    message = message,
                    showProgress = showProgress
                )
            }
        }
    }
}

/**
 * Dialog-based loading indicator
 */
@Composable
fun LoadingDialog(
    isLoading: Boolean,
    message: String = "Please wait...",
    onDismissRequest: () -> Unit = {},
    dismissOnBackPress: Boolean = false,
    dismissOnClickOutside: Boolean = false
) {
    if (isLoading) {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(
                dismissOnBackPress = dismissOnBackPress,
                dismissOnClickOutside = dismissOnClickOutside
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                LoadingContent(
                    message = message,
                    showProgress = true,
                    modifier = Modifier.padding(24.dp)
                )
            }
        }
    }
}

/**
 * Inline loading indicator for buttons or small areas
 */
@Composable
fun InlineLoadingIndicator(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    size: LoadingSize = LoadingSize.SMALL
) {
    AnimatedVisibility(
        visible = isLoading,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        val indicatorSize = when (size) {
            LoadingSize.SMALL -> 16.dp
            LoadingSize.MEDIUM -> 24.dp
            LoadingSize.LARGE -> 32.dp
        }
        
        CircularProgressIndicator(
            modifier = Modifier.size(indicatorSize),
            strokeWidth = when (size) {
                LoadingSize.SMALL -> 2.dp
                LoadingSize.MEDIUM -> 3.dp
                LoadingSize.LARGE -> 4.dp
            }
        )
    }
}

/**
 * Loading button that shows progress while performing an action
 */
@Composable
fun LoadingButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    text: String,
    loadingText: String = text
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled && !loading
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Loading indicator
            if (loading) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.alpha(1f)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(loadingText)
                }
            } else {
                Text(
                    text = text,
                    modifier = Modifier.alpha(1f)
                )
            }
        }
    }
}

/**
 * Skeleton loading placeholder
 */
@Composable
fun SkeletonLoader(
    modifier: Modifier = Modifier,
    shape: SkeletonShape = SkeletonShape.RECTANGLE
) {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton_alpha"
    )
    
    Box(
        modifier = modifier
            .clip(
                when (shape) {
                    SkeletonShape.RECTANGLE -> MaterialTheme.shapes.medium
                    SkeletonShape.CIRCLE -> CircleShape
                    SkeletonShape.ROUNDED -> MaterialTheme.shapes.large
                }
            )
            .background(
                MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
            )
    )
}

/**
 * List skeleton loader
 */
@Composable
fun ListSkeletonLoader(
    itemCount: Int = 3,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(itemCount) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SkeletonLoader(
                        modifier = Modifier.size(48.dp),
                        shape = SkeletonShape.CIRCLE
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SkeletonLoader(
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(16.dp)
                        )
                        SkeletonLoader(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Pulsating loading dots
 */
@Composable
fun LoadingDots(
    modifier: Modifier = Modifier,
    dotCount: Int = 3,
    dotSize: LoadingSize = LoadingSize.MEDIUM
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(
            when (dotSize) {
                LoadingSize.SMALL -> 4.dp
                LoadingSize.MEDIUM -> 6.dp
                LoadingSize.LARGE -> 8.dp
            }
        )
    ) {
        val delays = remember { List(dotCount) { it * 150 } }
        
        delays.forEachIndexed { index, delay ->
            var isAnimating by remember { mutableStateOf(false) }
            
            LaunchedEffect(Unit) {
                delay(delay.toLong())
                isAnimating = true
            }
            
            val scale by animateFloatAsState(
                targetValue = if (isAnimating) 1.2f else 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot_scale_$index"
            )
            
            Box(
                modifier = Modifier
                    .size(
                        when (dotSize) {
                            LoadingSize.SMALL -> 6.dp
                            LoadingSize.MEDIUM -> 8.dp
                            LoadingSize.LARGE -> 10.dp
                        }
                    )
                    .scale(scale)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

/**
 * Progress bar with label
 */
@Composable
fun LabeledProgressBar(
    progress: Float,
    label: String,
    modifier: Modifier = Modifier,
    showPercentage: Boolean = true
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            if (showPercentage) {
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Internal loading content component
 */
@Composable
private fun LoadingContent(
    message: String,
    showProgress: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (showProgress) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp)
            )
        }
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Loading size enum
 */
enum class LoadingSize {
    SMALL, MEDIUM, LARGE
}

/**
 * Skeleton shape enum
 */
enum class SkeletonShape {
    RECTANGLE, CIRCLE, ROUNDED
}
