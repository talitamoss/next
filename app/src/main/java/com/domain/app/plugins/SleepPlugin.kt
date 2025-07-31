package com.domain.app.plugins

import android.content.Context
import com.domain.app.core.data.DataPoint
import com.domain.app.core.plugin.*
import com.domain.app.core.plugin.security.*
import java.time.LocalTime
import java.time.Duration

/**
 * Sleep tracking plugin with security manifest
 */
class SleepPlugin : Plugin {
    override val id = "sleep"
    
    override val supportsAutomaticCollection = false

    override val metadata = PluginMetadata(
        name = "Sleep",
        description = "Track your sleep duration, quality, and dreams",
        version = "1.0.0",
        author = "System",
        category = PluginCategory.HEALTH,
        tags = listOf("sleep", "rest", "health", "recovery", "dreams"),
        dataPattern = DataPattern.COMPOSITE,
        inputType = InputType.DURATION,
        supportsMultiStage = true,
        relatedPlugins = listOf("mood", "energy", "exercise"),
        exportFormat = ExportFormat.CSV,
        dataSensitivity = DataSensitivity.SENSITIVE,
        naturalLanguageAliases = listOf(
            "sleep", "slept", "sleeping", "rest", "rested",
            "went to bed", "woke up", "nap", "napped",
            "insomnia", "couldn't sleep", "dream", "nightmare"
        ),
        contextualTriggers = listOf(
            ContextTrigger.TIME_OF_DAY,
            ContextTrigger.PATTERN_BASED
        )
    )
    
    override val securityManifest = PluginSecurityManifest(
        requestedCapabilities = setOf(
            PluginCapability.COLLECT_DATA,
            PluginCapability.READ_OWN_DATA,
            PluginCapability.LOCAL_STORAGE,
            PluginCapability.EXPORT_DATA,
            PluginCapability.ANALYTICS_BASIC
        ),
        dataSensitivity = DataSensitivity.SENSITIVE,
        dataAccess = setOf(DataAccessScope.OWN_DATA_ONLY),
        privacyPolicy = "Sleep data including dream journals is encrypted locally. Sleep patterns can reveal sensitive health information and are never shared without explicit consent.",
        dataRetention = DataRetentionPolicy.USER_CONTROLLED
    )
    
    override val trustLevel = PluginTrustLevel.OFFICIAL
    
    override fun getPermissionRationale() = mapOf(
        PluginCapability.COLLECT_DATA to "Record your sleep duration and quality",
        PluginCapability.READ_OWN_DATA to "View your sleep history and patterns",
        PluginCapability.LOCAL_STORAGE to "Save your sleep data securely on your device",
        PluginCapability.EXPORT_DATA to "Export sleep data for health analysis",
        PluginCapability.ANALYTICS_BASIC to "Calculate sleep trends and average sleep quality"
    )
    
    override suspend fun initialize(context: Context) {
        // No special initialization needed
    }
    
    override fun supportsManualEntry() = true
    
    override fun getQuickAddStages() = listOf(
        QuickAddStage(
            id = "duration",
            title = "How long did you sleep?",
            inputType = InputType.DURATION,
            required = true
        ),
        QuickAddStage(
            id = "quality",
            title = "How was your sleep quality?",
            inputType = InputType.CHOICE,
            required = true,
            options = listOf(
                QuickOption("Excellent", 5, "😴"),
                QuickOption("Good", 4, "😌"),
                QuickOption("Fair", 3, "😐"),
                QuickOption("Poor", 2, "😞"),
                QuickOption("Terrible", 1, "😫")
            )
        ),
        QuickAddStage(
            id = "dreams",
            title = "Any dreams to note?",
            inputType = InputType.TEXT,
            required = false,
            hint = "Optional dream journal"
        )
    )
    
    override fun getQuickAddConfig() = QuickAddConfig(
        title = "Quick Sleep Log",
        inputType = InputType.CHOICE,
        options = listOf(
            QuickOption("Last Night", "last_night", "🌙"),
            QuickOption("Nap", "nap", "💤"),
            QuickOption("Custom", "custom", "📝")
        )
    )
    
    override suspend fun createManualEntry(data: Map<String, Any>): DataPoint? {
        val duration = when (val value = data["duration"]) {
            is Number -> value.toLong()
            is String -> parseDuration(value)
            else -> null
        } ?: return null
        
        val quality = (data["quality"] as? Number)?.toInt() ?: 3
        val dreams = data["dreams"] as? String
        val type = data["type"] as? String ?: "night"
        
        return DataPoint(
            pluginId = id,
            type = "sleep_log",
            value = mapOf(
                "duration_minutes" to duration,
                "duration_hours" to (duration / 60.0),
                "quality" to quality,
                "quality_label" to getQualityLabel(quality),
                "type" to type,
                "dreams" to (dreams ?: ""),
                "has_dreams" to (dreams != null && dreams.isNotEmpty())
            ),
            metadata = mapOf(
                "quick_add" to "true",
                "sleep_type" to type
            ),
            source = "manual"
        )
    }
    
    override fun validateDataPoint(data: Map<String, Any>): ValidationResult {
        val duration = (data["duration"] as? Number)?.toLong()
        val quality = (data["quality"] as? Number)?.toInt()
        
        return when {
            duration == null -> ValidationResult.Error("Duration is required")
            duration <= 0 -> ValidationResult.Error("Duration must be positive")
            duration > 1440 -> ValidationResult.Error("Sleep duration cannot exceed 24 hours")
            quality != null && quality !in 1..5 -> ValidationResult.Error("Quality must be between 1 and 5")
            else -> ValidationResult.Success
        }
    }
    
    override fun exportHeaders() = listOf(
        "Date", "Time", "Duration (hours)", "Quality", "Type", "Dreams"
    )
    
    override fun formatForExport(dataPoint: DataPoint): Map<String, String> {
        val date = dataPoint.timestamp.toString().split("T")[0]
        val time = dataPoint.timestamp.toString().split("T")[1].split(".")[0]
        
        return mapOf(
            "Date" to date,
            "Time" to time,
            "Duration (hours)" to String.format("%.1f", dataPoint.value["duration_hours"] as? Double ?: 0.0),
            "Quality" to (dataPoint.value["quality_label"]?.toString() ?: ""),
            "Type" to (dataPoint.value["type"]?.toString() ?: ""),
            "Dreams" to (dataPoint.value["dreams"]?.toString() ?: "")
        )
    }
    
    private fun parseDuration(value: String): Long? {
        // Parse formats like "7h30m", "7.5h", "450m"
        return when {
            value.contains("h") && value.contains("m") -> {
                val parts = value.split("h")
                val hours = parts[0].toIntOrNull() ?: 0
                val minutes = parts[1].replace("m", "").toIntOrNull() ?: 0
                (hours * 60 + minutes).toLong()
            }
            value.endsWith("h") -> {
                val hours = value.replace("h", "").toDoubleOrNull() ?: return null
                (hours * 60).toLong()
            }
            value.endsWith("m") -> {
                value.replace("m", "").toLongOrNull()
            }
            else -> value.toLongOrNull()
        }
    }
    
    private fun getQualityLabel(quality: Int): String = when (quality) {
        5 -> "Excellent"
        4 -> "Good"
        3 -> "Fair"
        2 -> "Poor"
        1 -> "Terrible"
        else -> "Unknown"
    }
}
