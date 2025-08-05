package com.domain.app.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.domain.app.core.plugin.QuickOption
import com.domain.app.core.plugin.QuickAddStage
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseQuickAddDialog(
    stages: List<QuickAddStage>,
    onDismiss: () -> Unit,
    onComplete: (Map<String, Any>) -> Unit
) {
    var currentStageIndex by remember { mutableStateOf(0) }
    val currentStage = stages[currentStageIndex]
    val collectedData = remember { mutableStateMapOf<String, Any>() }
    
    // Stage-specific state
    var selectedActivity by remember { mutableStateOf<QuickOption?>(null) }
    var duration by remember { mutableStateOf(30f) }  // Default 30 minutes
    var selectedIntensity by remember { mutableStateOf<QuickOption?>(null) }
    var notes by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(currentStage.title)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp, max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (currentStage.id) {
                    "activity" -> {
                        // Clean grid layout for activities
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(currentStage.options ?: emptyList()) { option ->
                                FilterChip(
                                    selected = selectedActivity == option,
                                    onClick = { 
                                        selectedActivity = option
                                        collectedData["activity"] = option.value
                                    },
                                    label = { 
                                        Text(
                                            text = option.label,
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Center
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                    
                    "duration" -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Duration display
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = formatDuration(duration.roundToInt()),
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            
                            // Horizontal slider
                            Column {
                                Slider(
                                    value = duration,
                                    onValueChange = { 
                                        duration = it
                                        collectedData["duration"] = it.roundToInt()
                                    },
                                    valueRange = 5f..180f,  // 5 minutes to 3 hours
                                    steps = 34,  // 5-minute increments
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = MaterialTheme.colorScheme.primary,
                                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                                
                                // Quick select buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    listOf(15, 30, 45, 60, 90).forEach { minutes ->
                                        FilterChip(
                                            selected = duration.roundToInt() == minutes,
                                            onClick = { 
                                                duration = minutes.toFloat()
                                                collectedData["duration"] = minutes
                                            },
                                            label = { 
                                                Text(
                                                    if (minutes < 60) "${minutes}m" 
                                                    else "${minutes/60}h"
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                            
                            // Helpful text
                            Text(
                                text = "Slide to adjust duration or tap a quick option",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    
                    "intensity" -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            currentStage.options?.forEach { option ->
                                FilterChip(
                                    selected = selectedIntensity == option,
                                    onClick = { 
                                        selectedIntensity = option
                                        collectedData["intensity"] = option.value
                                    },
                                    label = { Text(option.label) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        
                        // Intensity descriptions
                        selectedIntensity?.let { intensity ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Text(
                                    text = getIntensityDescription(intensity.value as String),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                    
                    "notes" -> {
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { 
                                notes = it
                                collectedData["notes"] = it
                            },
                            label = { Text("Workout notes") },
                            placeholder = { Text("How did it feel? Any PRs?") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (currentStageIndex > 0) {
                    TextButton(
                        onClick = { currentStageIndex-- }
                    ) {
                        Text("Back")
                    }
                }
                
                TextButton(
                    onClick = {
                        if (currentStageIndex < stages.size - 1) {
                            // Move to next stage
                            currentStageIndex++
                        } else {
                            // Complete the flow
                            onComplete(collectedData.toMap())
                        }
                    },
                    enabled = when (currentStage.id) {
                        "activity" -> selectedActivity != null
                        "duration" -> true  // Always has a value from slider
                        "intensity" -> selectedIntensity != null
                        "notes" -> true  // Optional, always enabled
                        else -> true
                    }
                ) {
                    Text(
                        if (currentStageIndex < stages.size - 1) "Next" 
                        else "Save"
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatDuration(minutes: Int): String {
    return when {
        minutes < 60 -> "$minutes minutes"
        minutes % 60 == 0 -> "${minutes / 60} hour${if (minutes > 60) "s" else ""}"
        else -> "${minutes / 60}h ${minutes % 60}m"
    }
}

private fun getIntensityDescription(intensity: String): String {
    return when (intensity) {
        "light" -> "Easy pace, can maintain conversation comfortably"
        "moderate" -> "Breathing harder, can speak in short sentences"
        "intense" -> "Maximum effort, difficult to speak"
        else -> ""
    }
}
