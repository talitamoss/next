// app/src/main/java/com/domain/app/core/plugin/DefaultDataFormatter.kt
package com.domain.app.core.plugin

import com.domain.app.core.data.DataPoint

/**
 * Default formatter for plugins that don't provide custom formatting
 * Displays all non-hidden fields in a generic way
 */
class DefaultDataFormatter : PluginDataFormatter {
    
    override fun formatSummary(dataPoint: DataPoint): String {
        // Try to create a meaningful summary from common fields
        val value = dataPoint.value
        
        return when {
            // Check for common patterns
            value.containsKey("amount") -> {
                val amount = value["amount"]
                val unit = value["unit"] ?: ""
                "$amount $unit".trim()
            }
            value.containsKey("value") -> {
                val mainValue = value["value"]
                val unit = value["unit"] ?: ""
                "$mainValue $unit".trim()
            }
            value.containsKey("duration_seconds") -> {
                val seconds = (value["duration_seconds"] as? Number)?.toLong() ?: 0
                formatDuration(seconds)
            }
            value.containsKey("hours") -> {
                val hours = value["hours"]
                "$hours hours"
            }
            value.containsKey("note") && value["note"] != null -> {
                val note = value["note"].toString()
                if (note.length > 50) "${note.take(50)}..." else note
            }
            else -> {
                // Find first non-hidden field
                value.entries.firstOrNull { 
                    it.key !in getHiddenFields() && it.value != null
                }?.let { "${it.value}" } ?: "Data recorded"
            }
        }
    }
    
    override fun formatDetails(dataPoint: DataPoint): List<DataField> {
        val fields = mutableListOf<DataField>()
        val hiddenFields = getHiddenFields()
        
        dataPoint.value.forEach { (key, value) ->
            if (key !in hiddenFields && value != null) {
                fields.add(
                    DataField(
                        label = formatFieldName(key),
                        value = formatFieldValue(key, value),
                        isImportant = isImportantField(key),
                        isLongText = isLongTextField(key)
                    )
                )
            }
        }
        
        // Add note at the end if exists
        dataPoint.value["note"]?.let { note ->
            if (note.toString().isNotBlank()) {
                fields.add(
                    DataField(
                        label = "Note",
                        value = note.toString(),
                        isLongText = true
                    )
                )
            }
        }
        
        return fields
    }
    
    private fun formatFieldName(key: String): String {
        return key.split("_")
            .joinToString(" ") { word ->
                word.replaceFirstChar { it.uppercase() }
            }
    }
    
    private fun formatFieldValue(key: String, value: Any): String {
        return when {
            value is Boolean -> if (value) "Yes" else "No"
            key.contains("duration_seconds") -> {
                val seconds = (value as? Number)?.toLong() ?: 0
                formatDuration(seconds)
            }
            key.contains("_minutes") -> {
                val minutes = (value as? Number)?.toInt() ?: 0
                "$minutes minutes"
            }
            value is Number && value.toDouble() % 1 == 0.0 -> {
                value.toInt().toString()
            }
            else -> value.toString()
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
    
    private fun isImportantField(key: String): Boolean {
        return key in listOf("amount", "value", "hours", "duration", "score", "rating")
    }
    
    private fun isLongTextField(key: String): Boolean {
        return key in listOf("note", "notes", "description", "entry", "journal", "thoughts")
    }
}
