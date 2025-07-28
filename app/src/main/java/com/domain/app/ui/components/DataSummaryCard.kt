// app/src/main/java/com/domain/app/ui/components/DataSummaryCard.kt
package com.domain.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.domain.app.core.data.DataPoint
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun DataSummaryCard(
    title: String,
    dataPoints: List<DataPoint>,
    pluginId: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            when (pluginId) {
                "water" -> WaterSummary(dataPoints)
                "mood" -> MoodSummary(dataPoints)
                else -> GenericSummary(dataPoints)
            }
        }
    }
}

@Composable
fun WaterSummary(dataPoints: List<DataPoint>) {
    val today = LocalDate.now()
    val todayTotal = dataPoints
        .filter { 
            it.timestamp.atZone(ZoneId.systemDefault()).toLocalDate() == today 
        }
        .sumOf { 
            (it.value["amount"] as? Number)?.toDouble() ?: 0.0 
        }
    
    Column {
        Text("Today: ${todayTotal.toInt()} ml")
        LinearProgressIndicator(
            progress = (todayTotal / 2000f).coerceIn(0f, 1f),
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Goal: 2000 ml",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun MoodSummary(dataPoints: List<DataPoint>) {
    val recentMoods = dataPoints.take(7)
    val avgMood = if (recentMoods.isNotEmpty()) {
        recentMoods.mapNotNull { 
            (it.value["mood"] as? Number)?.toDouble() 
        }.average()
    } else 0.0
    
    Text("Recent average: %.1f".format(avgMood))
    Text(
        text = "Last 7 entries",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
fun GenericSummary(dataPoints: List<DataPoint>) {
    Text("Total entries: ${dataPoints.size}")
}
