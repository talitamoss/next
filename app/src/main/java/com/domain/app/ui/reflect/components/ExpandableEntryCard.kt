// app/src/main/java/com/domain/app/ui/reflect/components/ExpandableEntryCard.kt
package com.domain.app.ui.reflect.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.domain.app.ui.reflect.DataEntry
import com.domain.app.ui.theme.AppIcons
import com.domain.app.ui.utils.getPluginIconById  // FIXED: Added import
import java.time.format.DateTimeFormatter
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun ExpandableEntryCard(
    entry: DataEntry,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animation for chevron rotation
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(300),
        label = "chevron_rotation"
    )
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column {
            // Header (always visible)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Plugin Icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getPluginIconById(entry.pluginId),  // FIXED: Using utility function
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Main content
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = entry.pluginName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = entry.time,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = entry.displayValue,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    // Show note preview if not expanded and note exists
                    if (!isExpanded && !entry.note.isNullOrBlank()) {
                        Text(
                            text = "Note: ${entry.note}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
                
                // Expand/Collapse indicator
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(rotationAngle),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Expanded Content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(
                        start = 12.dp,
                        end = 12.dp,
                        bottom = 12.dp
                    )
                ) {
                    HorizontalDivider(  // FIXED: Changed from Divider to HorizontalDivider
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                    
                    // Raw Data Fields
                    RawDataSection(
                        rawValue = entry.rawValue,
                        metadata = entry.metadata
                    )
                    
                    // Full note if exists
                    if (!entry.note.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = "Note",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = entry.note,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    
                    // Metadata
                    if (entry.source != null || entry.metadata?.isNotEmpty() == true) {
                        Spacer(modifier = Modifier.height(12.dp))
                        MetadataSection(
                            source = entry.source,
                            timestamp = entry.timestamp,
                            metadata = entry.metadata
                        )
                    }
                    
                    // Action buttons
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ActionButton(
                            icon = Icons.Default.Edit,
                            label = "Edit",
                            onClick = onEdit
                        )
                        ActionButton(
                            icon = Icons.Default.Delete,
                            label = "Delete",
                            onClick = onDelete,
                            tint = MaterialTheme.colorScheme.error
                        )
                        ActionButton(
                            icon = Icons.Default.Share,
                            label = "Share",
                            onClick = onShare
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RawDataSection(
    rawValue: Map<String, Any?>,
    metadata: Map<String, Any?>?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Data Details",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
            
            // Display raw values, filtering out metadata fields
            rawValue.forEach { (key, value) ->
                if (key !in listOf("note", "metadata", "timestamp", "source") && value != null) {
                    DataField(
                        label = formatFieldName(key),
                        value = formatFieldValue(value, key)  // Pass the key for context-aware formatting
                    )
                }
            }
        }
    }
}

@Composable
private fun DataField(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun MetadataSection(
    source: String?,
    timestamp: java.time.Instant,
    metadata: Map<String, Any?>?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Metadata",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            
            source?.let {
                Text(
                    text = "Source: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Format timestamp nicely
            val localDateTime = timestamp.atZone(ZoneId.systemDefault()).toLocalDateTime()
            Text(
                text = "Recorded: ${localDateTime.format(DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a"))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Display other metadata
            metadata?.forEach { (key, value) ->
                if (key !in listOf("version", "inputType", "sensitivity") && value != null) {
                    Text(
                        text = "${formatFieldName(key)}: $value",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(
            contentColor = tint
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

// Format field names to be more user-friendly
private fun formatFieldName(key: String): String {
    return when (key) {
        // Food fields
        "mealType" -> "Meal Type"
        "foodCategory" -> "Food Type"
        "portion" -> "Portion Size"
        "satisfaction" -> "Satisfaction"
        
        // Movement/Exercise fields
        "type" -> "Activity Type"
        "duration" -> "Duration"
        "intensity" -> "Intensity"
        "duration_seconds" -> "Duration"
        "duration_minutes" -> "Duration"
        
        // Water fields
        "amount" -> "Amount"
        "unit" -> "Unit"
        
        // Mood fields
        "mood" -> "Mood"
        "score" -> "Score"
        "energy" -> "Energy Level"
        
        // Sleep fields
        "hours" -> "Hours Slept"
        "bedtime" -> "Bedtime"
        "waketime" -> "Wake Time"
        "quality" -> "Sleep Quality"
        "dreams" -> "Dreams"
        
        // Medical fields
        "medicine_name" -> "Medicine"
        "dosage_amount" -> "Dosage"
        "dosage_unit" -> "Unit"
        "time_taken" -> "Time Taken"
        "effectiveness" -> "Effectiveness"
        
        // Screen Time fields
        "device" -> "Device Type"
        "deviceType" -> "Device"
        "hours" -> "Hours"
        "feeling" -> "Feeling"
        "feeling_category" -> "Feeling"
        
        // Caffeine fields
        "caffeineType" -> "Type"
        "units" -> "Units"
        
        // Work fields
        "hours_worked" -> "Hours Worked"
        "productivity" -> "Productivity"
        "stress_level" -> "Stress Level"
        
        // Social fields
        "interaction_type" -> "Interaction Type"
        "people_count" -> "Number of People"
        "quality" -> "Quality"
        
        // Meditation fields
        "minutes" -> "Minutes"
        "technique" -> "Technique"
        "completed" -> "Completed"
        
        // Journal fields
        "entry" -> "Journal Entry"
        "tags" -> "Tags"
        
        // Audio fields
        "file_name" -> "Recording"
        
        // Poo fields
        "bristol_type" -> "Bristol Type"
        
        // Alcohol fields
        "drink_type" -> "Drink Type"
        
        // Generic fields
        "value" -> "Value"
        "notes" -> "Notes"
        "note" -> "Note"
        
        // Default case - manually capitalize words
        else -> key.split("_")
            .joinToString(" ") { word ->
                if (word.isNotEmpty()) {
                    word[0].uppercase() + word.substring(1).lowercase()
                } else {
                    ""
                }
            }
    }
}

// Format field values for display based on the field key and plugin context
private fun formatFieldValue(value: Any?, key: String = ""): String {
    return when {
        // Boolean values
        value is Boolean -> if (value) "Yes" else "No"
        
        // Time/Duration formatting
        key.contains("duration_seconds") -> {
            val seconds = (value as? Number)?.toLong() ?: 0
            formatDuration(seconds)
        }
        key.contains("duration_minutes") -> {
            val minutes = (value as? Number)?.toInt() ?: 0
            "$minutes minutes"
        }
        key == "bedtime" || key == "waketime" -> {
            val hour = (value as? Number)?.toFloat() ?: 0f
            formatTimeFromHour(hour)
        }
        
        // Food-specific formatting
        key == "mealType" -> when(value?.toString()) {
            "snack" -> "Snack"
            "light_meal" -> "Light Meal"
            "full_meal" -> "Full Meal"
            else -> value?.toString() ?: ""
        }
        key == "foodCategory" -> value?.toString()
            ?.replace("_", " ")
            ?.split(" ")
            ?.joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
            ?: ""
        key == "portion" -> {
            val portion = (value as? Number)?.toFloat() ?: 0f
            when {
                portion <= 0.5f -> "Small"
                portion <= 1.5f -> "Medium"
                portion <= 2.5f -> "Large"
                else -> "Extra Large"
            }
        }
        key == "satisfaction" -> formatSatisfaction(value)
        
        // Movement/Exercise formatting
        key == "intensity" -> formatIntensity(value)
        
        // Mood formatting
        key == "mood" -> formatMood(value?.toString())
        key == "energy" -> formatEnergyLevel(value)
        
        // Sleep formatting
        key == "quality" -> formatSleepQuality(value)
        
        // Screen time formatting
        key == "device" || key == "deviceType" -> formatDeviceType(value?.toString())
        key == "feeling" -> formatFeeling(value)
        key == "feeling_category" -> when(value?.toString()) {
            "very_negative" -> "Very Negative"
            "negative" -> "Negative"
            "neutral" -> "Neutral"
            "positive" -> "Positive"
            "very_positive" -> "Very Positive"
            else -> value?.toString() ?: ""
        }
        
        // Medical formatting
        key == "dosage_amount" -> {
            val amount = (value as? Number)?.toDouble() ?: 0.0
            if (amount == amount.toInt().toDouble()) {
                amount.toInt().toString()
            } else {
                amount.toString()
            }
        }
        
        // Poo formatting
        key == "bristol_type" -> formatBristolType(value)
        
        // Work formatting
        key == "productivity" -> formatProductivity(value)
        key == "stress_level" -> formatStressLevel(value)
        
        // Percentage or scale values
        key.contains("score") || key.contains("rating") -> {
            val score = (value as? Number)?.toInt() ?: 0
            "$score/10"
        }
        
        // Numeric values with potential units
        value is Number -> value.toString()
        
        // Default string handling
        else -> value?.toString() ?: ""
    }
}

// Helper formatting functions
private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    
    return when {
        hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, secs)
        minutes > 0 -> String.format("%d:%02d", minutes, secs)
        else -> String.format("%ds", secs)
    }
}

private fun formatTimeFromHour(hour: Float): String {
    val actualHour = hour.toInt() % 24
    val isPM = actualHour >= 12
    val displayHour = when (actualHour) {
        0 -> 12
        in 13..23 -> actualHour - 12
        else -> actualHour
    }
    return "$displayHour:00 ${if (isPM) "PM" else "AM"}"
}

private fun formatSatisfaction(value: Any?): String {
    val satisfaction = (value as? Number)?.toFloat() ?: return "Unknown"
    return when {
        satisfaction <= 0.2f -> "Still Hungry"
        satisfaction <= 0.4f -> "Somewhat Satisfied"
        satisfaction <= 0.6f -> "Satisfied"
        satisfaction <= 0.8f -> "Very Satisfied"
        else -> "Completely Full"
    }
}

private fun formatIntensity(value: Any?): String {
    return when (val intensity = value?.toString()) {
        "light" -> "Light"
        "moderate" -> "Moderate"
        "intense" -> "Intense"
        "very_intense" -> "Very Intense"
        else -> {
            // Handle numeric intensity
            val numIntensity = (value as? Number)?.toFloat() ?: return intensity ?: "Moderate"
            when {
                numIntensity <= 0.2f -> "Very Light"
                numIntensity <= 0.4f -> "Light"
                numIntensity <= 0.6f -> "Moderate"
                numIntensity <= 0.8f -> "Intense"
                else -> "Extreme"
            }
        }
    }
}

private fun formatMood(mood: String?): String {
    return if (mood != null && mood.isNotEmpty()) {
        mood[0].uppercase() + mood.substring(1).lowercase()
    } else {
        "Unknown"
    }
}

private fun formatEnergyLevel(value: Any?): String {
    val energy = (value as? Number)?.toInt() ?: return "Unknown"
    return when (energy) {
        in 0..2 -> "Very Low"
        in 3..4 -> "Low"
        in 5..6 -> "Medium"
        in 7..8 -> "High"
        in 9..10 -> "Very High"
        else -> "$energy/10"
    }
}

private fun formatSleepQuality(value: Any?): String {
    val quality = value?.toString()
    return if (quality != null && quality.isNotEmpty()) {
        quality[0].uppercase() + quality.substring(1).lowercase()
    } else {
        "Unknown"
    }
}

private fun formatDeviceType(device: String?): String {
    return when (device?.lowercase()) {
        "phone", "hand-held", "handheld" -> "Phone/Tablet"
        "laptop", "computer" -> "Laptop/Computer"
        "tv", "large_screen" -> "TV/Large Screen"
        else -> {
            device?.replace("_", " ")
                ?.split(" ")
                ?.joinToString(" ") { word ->
                    if (word.isNotEmpty()) {
                        word[0].uppercase() + word.substring(1).lowercase()
                    } else {
                        ""
                    }
                } ?: "Unknown"
        }
    }
}

private fun formatFeeling(value: Any?): String {
    val feeling = (value as? Number)?.toFloat() ?: return "Neutral"
    return when {
        feeling <= 2f -> "Very Negative"
        feeling <= 4f -> "Negative"
        feeling <= 6f -> "Neutral"
        feeling <= 8f -> "Positive"
        else -> "Very Positive"
    }
}

private fun formatBristolType(value: Any?): String {
    return when (value?.toString()) {
        "1", "Type 1" -> "Type 1: Pebbles"
        "2", "Type 2" -> "Type 2: Lumpy"
        "3", "Type 3" -> "Type 3: Cracked"
        "4", "Type 4" -> "Type 4: Smooth"
        "5", "Type 5" -> "Type 5: Soft Blobs"
        "6", "Type 6" -> "Type 6: Mushy"
        "7", "Type 7" -> "Type 7: Liquid"
        else -> value?.toString() ?: "Unknown"
    }
}

private fun formatProductivity(value: Any?): String {
    val prod = (value as? Number)?.toFloat() ?: return "Unknown"
    return when {
        prod <= 0.2f -> "Very Low"
        prod <= 0.4f -> "Low"
        prod <= 0.6f -> "Moderate"
        prod <= 0.8f -> "High"
        else -> "Very High"
    }
}

private fun formatStressLevel(value: Any?): String {
    val stress = (value as? Number)?.toFloat() ?: return "Unknown"
    return when {
        stress <= 0.2f -> "Very Low"
        stress <= 0.4f -> "Low"
        stress <= 0.6f -> "Moderate"
        stress <= 0.8f -> "High"
        else -> "Very High"
    }
}

// REMOVED duplicate getPluginIcon function - now using import from PluginUtils
