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
                        imageVector = getPluginIcon(entry.pluginId),
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
                    Divider(
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
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
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
            
            // Exact timestamp
            val dateTime = timestamp.atZone(ZoneId.systemDefault()).toLocalDateTime()
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            Text(
                text = "Recorded: ${dateTime.format(formatter)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Source
            if (source != null) {
                Text(
                    text = "Source: $source",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Additional metadata
            metadata?.forEach { (key, value) ->
                if (key !in listOf("note", "source") && value != null) {
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

// Helper function to get appropriate icon for plugin
private fun getPluginIcon(pluginId: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (pluginId) {
        "mood" -> Icons.Default.Mood
        "exercise", "movement" -> Icons.Default.DirectionsRun
        "sleep" -> Icons.Default.Bedtime
        "medication", "medical" -> Icons.Default.MedicalServices
        "screen_time" -> Icons.Default.PhoneAndroid
        "water" -> Icons.Default.WaterDrop
        "food" -> Icons.Default.Restaurant
        "caffeine" -> Icons.Default.LocalCafe
        "work" -> Icons.Default.Work
        "social" -> Icons.Default.People
        "meditation" -> Icons.Default.SelfImprovement
        "journal" -> Icons.Default.Book
        "audio" -> Icons.Default.Mic
        "poo" -> Icons.Default.Wc
        "alcohol" -> Icons.Default.LocalBar
        else -> Icons.Default.Extension
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
        
        // Water fields
        "amount" -> "Amount"
        "unit" -> "Unit"
        
        // Mood fields
        "mood" -> "Mood"
        "score" -> "Score"
        "energy" -> "Energy Level"
        
        // Sleep fields
        "hours" -> "Hours Slept"
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
        
        // Caffeine fields
        "caffeineType" -> "Type"
        
        // Alcohol fields
        "drink_type" -> "Drink Type"
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
        
        // Journal fields
        "entry" -> "Journal Entry"
        "tags" -> "Tags"
        
        // Audio fields
        "file_name" -> "Recording"
        
        // Poo fields
        "bristol_type" -> "Bristol Type"
        
        // Generic fields
        "value" -> "Value"
        "notes" -> "Notes"
        "note" -> "Note"
        
        // Default case - manually capitalize words instead of using replaceFirstChar
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
        // Food-specific formatting
        key == "mealType" -> when(value?.toString()) {
            "snack" -> "Snack"
            "light_meal" -> "Light Meal"
            "full_meal" -> "Full Meal"
            else -> value?.toString() ?: "N/A"
        }
        key == "foodCategory" -> formatFoodCategory(value?.toString())
        key == "portion" -> formatPortionSize(value)
        key == "satisfaction" -> formatSatisfactionLevel(value)
        
        // Movement/Exercise formatting
        key == "type" && value is String -> value
        key == "duration" && value is Number -> "${value.toInt()} minutes"
        key == "intensity" -> formatIntensityLevel(value)
        
        // Water formatting - Fixed null safety
        key == "amount" && value is Number -> value.toInt().toString()
        key == "unit" -> {
            val unitStr = value?.toString()
            when (unitStr) {
                "ml" -> "ml"
                "oz" -> "oz"
                "cups" -> "cups"
                "liters" -> "liters"
                null -> ""
                else -> unitStr
            }
        }
        
        // Mood formatting
        key == "mood" -> formatMood(value?.toString())
        key == "score" && value is Number -> "${value.toInt()}/10"
        key == "energy" && value is Number -> formatEnergyLevel(value)
        
        // Sleep formatting
        key == "hours" && value is Number -> {
            val hours = value.toDouble()
            if (hours == hours.toInt().toDouble()) {
                "${hours.toInt()} hours"
            } else {
                String.format("%.1f hours", hours)
            }
        }
        key == "quality" -> formatSleepQuality(value)
        key == "dreams" && value is Boolean -> if (value) "Yes, had dreams" else "No dreams"
        
        // Medical formatting
        key == "medicine_name" -> value?.toString() ?: "Unknown"
        key == "dosage_amount" -> value?.toString() ?: "0"
        key == "effectiveness" -> formatEffectiveness(value)
        
        // Screen Time formatting
        key in listOf("device", "deviceType") -> formatDeviceType(value?.toString())
        key == "feeling" && value is Number -> formatFeeling(value)
        
        // Caffeine formatting
        key == "caffeineType" || (key == "type" && value in listOf("Coffee", "Tea", "Energy Drink", "Soda", "Other")) -> 
            value?.toString() ?: "Unknown"
        
        // Work formatting
        key == "productivity" -> formatProductivity(value)
        key == "stress_level" -> formatStressLevel(value)
        
        // Social formatting
        key == "interaction_type" -> formatInteractionType(value?.toString())
        key == "people_count" && value is Number -> "${value.toInt()} people"
        key == "quality" -> formatQuality(value)
        
        // Meditation formatting
        key == "minutes" && value is Number -> "${value.toInt()} minutes"
        key == "technique" -> {
            val tech = value?.toString()
            if (tech != null) {
                tech.replace("_", " ")
                    .split(" ")
                    .joinToString(" ") { word ->
                        if (word.isNotEmpty()) {
                            word[0].uppercase() + word.substring(1).lowercase()
                        } else {
                            ""
                        }
                    }
            } else {
                "Unknown"
            }
        }
        
        // Bristol Stool Chart formatting
        key == "bristol_type" -> formatBristolType(value)
        
        // Audio formatting
        key == "duration_seconds" && value is Number -> formatDuration(value.toLong())
        key == "file_name" -> "Audio Recording"
        
        // Journal formatting
        key == "entry" && value is String -> if (value.length > 100) {
            "${value.take(100)}..."
        } else value
        key == "tags" && value is List<*> -> value.joinToString(", ")
        
        // Default formatting
        value is Number -> formatNumber(value)
        value is Boolean -> if (value) "Yes" else "No"
        value is List<*> -> value.joinToString(", ")
        value is Map<*, *> -> value.entries.joinToString("\n") { "${it.key}: ${it.value}" }
        else -> {
            val str = value?.toString()
            if (str != null) {
                str.replace("_", " ")
                    .split(" ")
                    .joinToString(" ") { word ->
                        if (word.isNotEmpty()) {
                            word[0].uppercase() + word.substring(1).lowercase()
                        } else {
                            ""
                        }
                    }
            } else {
                "N/A"
            }
        }
    }
}

// Helper formatting functions
private fun formatFoodCategory(category: String?): String {
    return when (category) {
        // Snacks
        "nuts" -> "Nuts"
        "fruit" -> "Fruit"
        "baked_goods" -> "Baked Goods"
        "protein_bar" -> "Protein Bar"
        "chips" -> "Chips"
        "candy" -> "Candy"
        "yogurt" -> "Yogurt"
        "vegetables" -> "Vegetables"
        "cheese" -> "Cheese"
        
        // Light Meals
        "carb_heavy" -> "Carb Heavy"
        "protein_focused" -> "Protein Focused"
        "salad" -> "Salad"
        "soup" -> "Soup"
        "sandwich" -> "Sandwich"
        "smoothie" -> "Smoothie"
        "leftovers" -> "Leftovers"
        "fast_food" -> "Fast Food"
        
        // Full Meals
        "balanced" -> "Balanced Meal"
        "protein_heavy" -> "Protein Heavy"
        "vegetarian" -> "Vegetarian"
        "vegan" -> "Vegan"
        "pasta_rice" -> "Pasta/Rice"
        "pizza" -> "Pizza"
        "restaurant" -> "Restaurant"
        "home_cooked" -> "Home Cooked"
        "takeout" -> "Takeout"
        
        else -> {
            if (category != null) {
                category.replace("_", " ")
                    .split(" ")
                    .joinToString(" ") { word ->
                        if (word.isNotEmpty()) {
                            word[0].uppercase() + word.substring(1).lowercase()
                        } else {
                            ""
                        }
                    }
            } else {
                "Unknown"
            }
        }
    }
}

private fun formatPortionSize(value: Any?): String {
    val portion = (value as? Number)?.toFloat() ?: return "Normal"
    return when {
        portion <= 0.5f -> "Very Small"
        portion <= 0.75f -> "Small"
        portion <= 1.25f -> "Normal"
        portion <= 2.0f -> "Large"
        else -> "Very Large"
    }
}

private fun formatSatisfactionLevel(value: Any?): String {
    val satisfaction = (value as? Number)?.toFloat() ?: return "Neutral"
    return when {
        satisfaction <= 0.2f -> "Still Very Hungry"
        satisfaction <= 0.4f -> "Still Hungry"
        satisfaction <= 0.6f -> "Neutral"
        satisfaction <= 0.8f -> "Satisfied"
        else -> "Very Satisfied"
    }
}

private fun formatIntensityLevel(value: Any?): String {
    val intensity = (value as? Number)?.toFloat() ?: return "Moderate"
    return when {
        intensity <= 0.2f -> "Very Light"
        intensity <= 0.4f -> "Light"
        intensity <= 0.6f -> "Moderate"
        intensity <= 0.8f -> "Intense"
        else -> "Extreme"
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
        "tv", "large screen", "large_screen" -> "TV/Large Screen"
        else -> {
            if (device != null) {
                device.replace("_", " ")
                    .split(" ")
                    .joinToString(" ") { word ->
                        if (word.isNotEmpty()) {
                            word[0].uppercase() + word.substring(1).lowercase()
                        } else {
                            ""
                        }
                    }
            } else {
                "Unknown"
            }
        }
    }
}

private fun formatFeeling(value: Any?): String {
    val feeling = (value as? Number)?.toFloat() ?: return "Neutral"
    return when {
        feeling <= 0.2f -> "Very Negative"
        feeling <= 0.4f -> "Negative"
        feeling <= 0.6f -> "Neutral"
        feeling <= 0.8f -> "Positive"
        else -> "Very Positive"
    }
}

private fun formatBristolType(value: Any?): String {
    return when (value?.toString()) {
        "1", "Type 1" -> "Type 1: Pebbles (Separate hard lumps)"
        "2", "Type 2" -> "Type 2: Lumpy (Sausage-shaped but lumpy)"
        "3", "Type 3" -> "Type 3: Dry (Sausage with cracks)"
        "4", "Type 4" -> "Type 4: Smooth (Ideal)"
        "5", "Type 5" -> "Type 5: Blobs (Soft with clear edges)"
        "6", "Type 6" -> "Type 6: Mushy (Fluffy pieces)"
        "7", "Type 7" -> "Type 7: Liquid (Watery)"
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

private fun formatInteractionType(type: String?): String {
    return when (type?.lowercase()) {
        "in_person" -> "In Person"
        "video_call" -> "Video Call"
        "phone_call" -> "Phone Call"
        "text" -> "Text/Chat"
        "group" -> "Group"
        else -> {
            if (type != null) {
                type.replace("_", " ")
                    .split(" ")
                    .joinToString(" ") { word ->
                        if (word.isNotEmpty()) {
                            word[0].uppercase() + word.substring(1).lowercase()
                        } else {
                            ""
                        }
                    }
            } else {
                "Unknown"
            }
        }
    }
}

private fun formatQuality(value: Any?): String {
    val quality = value?.toString()
    return if (quality != null && quality.isNotEmpty()) {
        quality[0].uppercase() + quality.substring(1).lowercase()
    } else {
        "Unknown"
    }
}

private fun formatEffectiveness(value: Any?): String {
    return when (value?.toString()?.lowercase()) {
        "not_effective" -> "Not Effective"
        "slightly_effective" -> "Slightly Effective"
        "moderately_effective" -> "Moderately Effective"
        "very_effective" -> "Very Effective"
        else -> {
            val eff = value?.toString()
            if (eff != null) {
                eff.replace("_", " ")
                    .split(" ")
                    .joinToString(" ") { word ->
                        if (word.isNotEmpty()) {
                            word[0].uppercase() + word.substring(1).lowercase()
                        } else {
                            ""
                        }
                    }
            } else {
                "Unknown"
            }
        }
    }
}

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

private fun formatNumber(value: Number): String {
    return if (value.toDouble() % 1 == 0.0) {
        value.toInt().toString()
    } else {
        String.format("%.2f", value.toDouble())
    }
}
