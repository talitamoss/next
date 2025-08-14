// app/src/main/java/com/domain/app/ui/components/core/cards/MetricCard.kt
package com.domain.app.ui.components.core.cards

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A reusable metric card component for displaying plugin data on the dashboard.
 * Replaces duplicate card implementations across different screens.
 * 
 * @param title The title of the metric
 * @param value The current value to display
 * @param subtitle Optional subtitle text
 * @param icon Optional icon for the metric
 * @param progress Optional progress value (0f to 1f)
 * @param trend Optional trend indicator (+1 for up, -1 for down, 0 for neutral)
 * @param onClick Optional click handler
 * @param modifier Modifier for the card
 * @param colors Custom colors for the card
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconEmoji: String? = null,
    progress: Float? = null,
    trend: Int? = null,
    onClick: (() -> Unit)? = null,
    colors: MetricCardColors = MetricCardDefaults.colors(),
    size: MetricCardSize = MetricCardSize.MEDIUM
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress ?: 0f,
        animationSpec = tween(durationMillis = 600),
        label = "progress"
    )
    
    val trendColor by animateColorAsState(
        targetValue = when (trend) {
            1 -> colors.trendUpColor
            -1 -> colors.trendDownColor
            else -> colors.neutralColor
        },
        label = "trend_color"
    )
    
    Card(
        modifier = modifier
            .size(
                width = when (size) {
                    MetricCardSize.SMALL -> 120.dp
                    MetricCardSize.MEDIUM -> 160.dp
                    MetricCardSize.LARGE -> 200.dp
                },
                height = when (size) {
                    MetricCardSize.SMALL -> 120.dp
                    MetricCardSize.MEDIUM -> 140.dp
                    MetricCardSize.LARGE -> 160.dp
                }
            )
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick() }
                } else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.backgroundColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 8.dp
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Progress background if provided
            progress?.let {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    colors.progressColor.copy(alpha = 0.1f),
                                    colors.progressColor.copy(alpha = 0.2f)
                                )
                            )
                        )
                )
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header with icon and title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.titleColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        subtitle?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.subtitleColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    
                    // Icon or Emoji
                    when {
                        icon != null -> {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = colors.iconColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        iconEmoji != null -> {
                            Text(
                                text = iconEmoji,
                                fontSize = 20.sp
                            )
                        }
                    }
                }
                
                // Value display
                Column {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = value,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = when (size) {
                                    MetricCardSize.SMALL -> 20.sp
                                    MetricCardSize.MEDIUM -> 24.sp
                                    MetricCardSize.LARGE -> 28.sp
                                }
                            ),
                            fontWeight = FontWeight.Bold,
                            color = colors.valueColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        // Trend indicator
                        trend?.let {
                            Text(
                                text = when (it) {
                                    1 -> "↑"
                                    -1 -> "↓"
                                    else -> "→"
                                },
                                color = trendColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    // Progress bar if progress is provided
                    progress?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = animatedProgress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = colors.progressColor,
                            trackColor = colors.progressTrackColor
                        )
                    }
                }
            }
            
            // Click indicator
            if (onClick != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(colors.clickableIndicatorColor)
                )
            }
        }
    }
}

/**
 * Size variants for MetricCard
 */
enum class MetricCardSize {
    SMALL,
    MEDIUM,
    LARGE
}

/**
 * Colors configuration for MetricCard
 */
data class MetricCardColors(
    val backgroundColor: Color,
    val titleColor: Color,
    val subtitleColor: Color,
    val valueColor: Color,
    val iconColor: Color,
    val progressColor: Color,
    val progressTrackColor: Color,
    val trendUpColor: Color,
    val trendDownColor: Color,
    val neutralColor: Color,
    val clickableIndicatorColor: Color
)

/**
 * Default configurations for MetricCard
 */
object MetricCardDefaults {
    @Composable
    fun colors(
        backgroundColor: Color = MaterialTheme.colorScheme.surface,
        titleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        valueColor: Color = MaterialTheme.colorScheme.onSurface,
        iconColor: Color = MaterialTheme.colorScheme.primary,
        progressColor: Color = MaterialTheme.colorScheme.primary,
        progressTrackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
        trendUpColor: Color = Color(0xFF4CAF50),
        trendDownColor: Color = Color(0xFFF44336),
        neutralColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        clickableIndicatorColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    ) = MetricCardColors(
        backgroundColor = backgroundColor,
        titleColor = titleColor,
        subtitleColor = subtitleColor,
        valueColor = valueColor,
        iconColor = iconColor,
        progressColor = progressColor,
        progressTrackColor = progressTrackColor,
        trendUpColor = trendUpColor,
        trendDownColor = trendDownColor,
        neutralColor = neutralColor,
        clickableIndicatorColor = clickableIndicatorColor
    )
}

/**
 * Grid layout for multiple metric cards
 */
@Composable
fun MetricCardGrid(
    cards: List<MetricCardData>,
    modifier: Modifier = Modifier,
    columns: Int = 2,
    onClick: ((MetricCardData) -> Unit)? = null
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        cards.chunked(columns).forEach { rowCards ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowCards.forEach { card ->
                    MetricCard(
                        title = card.title,
                        value = card.value,
                        subtitle = card.subtitle,
                        icon = card.icon,
                        iconEmoji = card.iconEmoji,
                        progress = card.progress,
                        trend = card.trend,
                        onClick = onClick?.let { { it(card) } },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Fill remaining space if row is not complete
                repeat(columns - rowCards.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * Data class for metric card information
 */
data class MetricCardData(
    val id: String,
    val title: String,
    val value: String,
    val subtitle: String? = null,
    val icon: ImageVector? = null,
    val iconEmoji: String? = null,
    val progress: Float? = null,
    val trend: Int? = null,
    val metadata: Map<String, Any> = emptyMap()
)
