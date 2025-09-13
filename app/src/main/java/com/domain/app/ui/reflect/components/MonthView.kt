// app/src/main/java/com/domain/app/ui/reflect/components/MonthView.kt
package com.domain.app.ui.reflect.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.domain.app.ui.reflect.MonthData
import com.domain.app.ui.reflect.WeekStats
import com.domain.app.ui.theme.AppIcons
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

@Composable
fun MonthView(
    monthData: MonthData?,
    selectedPluginIds: Set<String>,
    availablePlugins: Map<String, String>,
    modifier: Modifier = Modifier
) {
    if (monthData == null) {
        EmptyMonthView(modifier)
        return
    }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Month header
        MonthHeader(yearMonth = monthData.yearMonth)
        
        // Summary statistics
        MonthSummaryCard(
            monthData = monthData,
            selectedPluginIds = selectedPluginIds,
            availablePlugins = availablePlugins
        )
        
        // Weekly breakdown
        Text(
            text = "Weekly Breakdown",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        monthData.weeklyBreakdown.forEach { (weekNumber, weekStats) ->
            WeekBreakdownCard(
                weekNumber = weekNumber,
                stats = weekStats,
                yearMonth = monthData.yearMonth
            )
        }
        
        // Trends and patterns
        if (monthData.trends.isNotEmpty()) {
            TrendsCard(
                trends = monthData.trends,
                selectedPluginIds = selectedPluginIds,
                availablePlugins = availablePlugins
            )
        }
        
        // Best and worst days
        BestWorstDaysCard(
            bestDay = monthData.bestDay,
            worstDay = monthData.worstDay
        )
    }
}

@Composable
private fun MonthHeader(yearMonth: YearMonth) {
    val formatter = DateTimeFormatter.ofPattern("MMMM yyyy")
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = yearMonth.format(formatter),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${yearMonth.lengthOfMonth()} days",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MonthSummaryCard(
    monthData: MonthData,
    selectedPluginIds: Set<String>,
    availablePlugins: Map<String, String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Month Overview",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            
            // Key metrics in a grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricBox(
                    label = "Total",
                    value = monthData.totalEntries.toString(),
                    modifier = Modifier.weight(1f)
                )
                MetricBox(
                    label = "Daily Avg",
                    value = "%.1f".format(monthData.dailyAverage),
                    modifier = Modifier.weight(1f)
                )
                MetricBox(
                    label = "Active Days",
                    value = "${monthData.activeDays}",
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Plugin breakdown for selected plugins
            if (selectedPluginIds.isNotEmpty() && selectedPluginIds.size <= 3) {
                Divider(modifier = Modifier.padding(vertical = 4.dp))
                
                Text(
                    text = "Activity Breakdown",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                
                selectedPluginIds.forEach { pluginId ->
                    val pluginName = availablePlugins[pluginId] ?: pluginId
                    val pluginTotal = monthData.pluginTotals[pluginId] ?: 0
                    val percentage = if (monthData.totalEntries > 0) {
                        (pluginTotal.toFloat() / monthData.totalEntries * 100).toInt()
                    } else 0
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = pluginName,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        
                        Box(
                            modifier = Modifier
                                .width(100.dp)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(percentage / 100f)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                        
                        Text(
                            text = "$pluginTotal",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(40.dp),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WeekBreakdownCard(
    weekNumber: Int,
    stats: WeekStats,
    yearMonth: YearMonth
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Week $weekNumber",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${stats.startDate.dayOfMonth}-${stats.endDate.dayOfMonth} ${yearMonth.month.name.take(3)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Activity intensity indicator
                val intensity = when (stats.totalEntries) {
                    0 -> 0f
                    in 1..10 -> 0.3f
                    in 11..25 -> 0.6f
                    else -> 1f
                }
                
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = intensity)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stats.totalEntries.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (intensity > 0.5f) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                }
                
                // Trend indicator - FIXED: Using correct icon references
                if (stats.trend != null) {
                    Icon(
                        imageVector = when {
                            stats.trend > 0.1f -> AppIcons.Data.trending
                            stats.trend < -0.1f -> AppIcons.Data.trendingDown
                            else -> Icons.Default.Remove
                        },
                        contentDescription = "Trend",
                        tint = when {
                            stats.trend > 0.1f -> Color.Green
                            stats.trend < -0.1f -> Color.Red
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TrendsCard(
    trends: Map<String, Float>,
    selectedPluginIds: Set<String>,
    availablePlugins: Map<String, String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Monthly Trends",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            
            trends.forEach { (metric, change) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = metric,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (change >= 0) {
                                AppIcons.Data.trending
                            } else {
                                AppIcons.Data.trendingDown
                            },
                            contentDescription = null,
                            tint = if (change >= 0) Color.Green else Color.Red,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${if (change >= 0) "+" else ""}%.1f%%".format(change),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (change >= 0) Color.Green else Color.Red
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BestWorstDaysCard(
    bestDay: Pair<String, Int>?,
    worstDay: Pair<String, Int>?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Best day
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = Color.Green.copy(alpha = 0.1f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = AppIcons.Action.favorite,
                    contentDescription = "Best Day",
                    tint = Color.Green,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Most Active",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = bestDay?.first ?: "N/A",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${bestDay?.second ?: 0} entries",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Worst day
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = Color.Red.copy(alpha = 0.1f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Least Active",
                    tint = Color.Red,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Least Active",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = worstDay?.first ?: "N/A",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${worstDay?.second ?: 0} entries",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyMonthView(modifier: Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = AppIcons.Data.calendar,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                text = "No data for this month",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Start tracking to see monthly insights",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
