// app/src/main/java/com/domain/app/ui/components/core/progress/CircularProgress.kt
package com.domain.app.ui.components.core.progress

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.progressSemantics
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/**
 * A customizable circular progress indicator for displaying progress or stats.
 * Can be used for showing completion percentages, goals, or any circular metrics.
 * 
 * @param progress Progress value between 0f and 1f
 * @param modifier Modifier for the component
 * @param size Size of the circular progress
 * @param strokeWidth Width of the progress stroke
 * @param animateToProgress Whether to animate to the progress value
 * @param showPercentage Whether to show percentage text in center
 * @param label Optional label below the percentage
 * @param colors Custom colors for the progress
 * @param startAngle Starting angle for the progress arc (default -90 for top)
 * @param maxAngle Maximum angle for the progress arc (default 360 for full circle)
 * @param backgroundStroke Whether to show background stroke
 * @param segmented Whether to show segmented progress
 * @param segments Number of segments if segmented
 */
@Composable
fun CircularProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    strokeWidth: Dp = 8.dp,
    animateToProgress: Boolean = true,
    showPercentage: Boolean = true,
    label: String? = null,
    colors: CircularProgressColors = CircularProgressDefaults.colors(),
    startAngle: Float = -90f,
    maxAngle: Float = 360f,
    backgroundStroke: Boolean = true,
    segmented: Boolean = false,
    segments: Int = 12
) {
    val animatedProgress by animateFloatAsState(
        targetValue = if (animateToProgress) progress else progress,
        animationSpec = tween(
            durationMillis = 1000,
            easing = FastOutSlowInEasing
        ),
        label = "progress"
    )
    
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .progressSemantics(progress)
        ) {
            val stroke = Stroke(
                width = strokeWidth.toPx(),
                cap = StrokeCap.Round
            )
            
            // Draw background circle/arc
            if (backgroundStroke) {
                drawArc(
                    color = colors.backgroundColor,
                    startAngle = startAngle,
                    sweepAngle = maxAngle,
                    useCenter = false,
                    style = stroke
                )
            }
            
            // Draw progress
            if (segmented) {
                drawSegmentedProgress(
                    progress = animatedProgress,
                    startAngle = startAngle,
                    maxAngle = maxAngle,
                    segments = segments,
                    strokeWidth = strokeWidth.toPx(),
                    progressColor = colors.progressColor,
                    backgroundColor = colors.backgroundColor
                )
            } else {
                drawArc(
                    brush = colors.progressBrush ?: SolidColor(colors.progressColor),
                    startAngle = startAngle,
                    sweepAngle = maxAngle * animatedProgress,
                    useCenter = false,
                    style = stroke
                )
            }
        }
        
        // Center content
        if (showPercentage || label != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (showPercentage) {
                    Text(
                        text = "${(animatedProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.textColor,
                        textAlign = TextAlign.Center
                    )
                }
                label?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.labelColor,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Circular progress with icon in center
 */
@Composable
fun CircularProgressWithIcon(
    progress: Float,
    emoji: String,
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    strokeWidth: Dp = 8.dp,
    label: String? = null,
    colors: CircularProgressColors = CircularProgressDefaults.colors()
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        CircularProgress(
            progress = progress,
            size = size,
            strokeWidth = strokeWidth,
            showPercentage = false,
            colors = colors
        )
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = emoji,
                fontSize = (size.value / 3).sp
            )
            label?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.labelColor
                )
            }
        }
    }
}

/**
 * Multi-ring circular progress for multiple values
 */
@Composable
fun MultiRingCircularProgress(
    progressValues: List<ProgressData>,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    ringWidth: Dp = 8.dp,
    ringSpacing: Dp = 4.dp,
    centerContent: @Composable BoxScope.() -> Unit = {}
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            progressValues.forEachIndexed { index, data ->
                val currentRingWidth = ringWidth.toPx()
                val spacing = ringSpacing.toPx()
                val radius = (this.size.minDimension / 2) - 
                           (index * (currentRingWidth + spacing)) - 
                           (currentRingWidth / 2)
                
                val stroke = Stroke(
                    width = currentRingWidth,
                    cap = StrokeCap.Round
                )
                
                // Background ring
                drawCircle(
                    color = data.backgroundColor,
                    radius = radius,
                    style = stroke
                )
                
                // Progress arc
                drawArc(
                    color = data.color,
                    startAngle = -90f,
                    sweepAngle = 360f * data.progress,
                    useCenter = false,
                    topLeft = Offset(
                        this.size.width / 2 - radius,
                        this.size.height / 2 - radius
                    ),
                    size = Size(radius * 2, radius * 2),
                    style = stroke
                )
            }
        }
        
        centerContent()
    }
}

/**
 * Animated circular counter
 */
@Composable
fun CircularCounter(
    current: Int,
    target: Int,
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    label: String? = null,
    colors: CircularProgressColors = CircularProgressDefaults.colors()
) {
    val progress = if (target > 0) current.toFloat() / target else 0f
    
    CircularProgress(
        progress = progress.coerceIn(0f, 1f),
        modifier = modifier,
        size = size,
        showPercentage = false,
        label = label,
        colors = colors
    )
    
    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$current",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textColor
            )
            Text(
                text = "of $target",
                style = MaterialTheme.typography.labelSmall,
                color = colors.labelColor
            )
        }
    }
}

// Helper function to draw segmented progress
private fun DrawScope.drawSegmentedProgress(
    progress: Float,
    startAngle: Float,
    maxAngle: Float,
    segments: Int,
    strokeWidth: Float,
    progressColor: Color,
    backgroundColor: Color
) {
    val segmentAngle = maxAngle / segments
    val segmentGap = 2f // degrees
    
    repeat(segments) { index ->
        val segmentProgress = ((progress * segments) - index).coerceIn(0f, 1f)
        val color = if (segmentProgress > 0) progressColor else backgroundColor
        
        drawArc(
            color = color.copy(alpha = if (segmentProgress > 0) 1f else 0.3f),
            startAngle = startAngle + (index * segmentAngle) + segmentGap / 2,
            sweepAngle = segmentAngle - segmentGap,
            useCenter = false,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round
            )
        )
    }
}

/**
 * Data class for multi-ring progress
 */
data class ProgressData(
    val progress: Float,
    val color: Color,
    val backgroundColor: Color,
    val label: String? = null
)

/**
 * Colors for circular progress
 */
data class CircularProgressColors(
    val progressColor: Color,
    val backgroundColor: Color,
    val textColor: Color,
    val labelColor: Color,
    val progressBrush: Brush? = null
)

/**
 * Default colors for circular progress
 */
object CircularProgressDefaults {
    @Composable
    fun colors(
        progressColor: Color = MaterialTheme.colorScheme.primary,
        backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
        textColor: Color = MaterialTheme.colorScheme.onSurface,
        labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        progressBrush: Brush? = null
    ): CircularProgressColors {
        return CircularProgressColors(
            progressColor = progressColor,
            backgroundColor = backgroundColor,
            textColor = textColor,
            labelColor = labelColor,
            progressBrush = progressBrush
        )
    }
    
    @Composable
    fun gradientColors(
        startColor: Color = MaterialTheme.colorScheme.primary,
        endColor: Color = MaterialTheme.colorScheme.tertiary,
        backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
        textColor: Color = MaterialTheme.colorScheme.onSurface,
        labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
    ): CircularProgressColors {
        return CircularProgressColors(
            progressColor = startColor,
            backgroundColor = backgroundColor,
            textColor = textColor,
            labelColor = labelColor,
            progressBrush = Brush.sweepGradient(
                colors = listOf(startColor, endColor)
            )
        )
    }
}
