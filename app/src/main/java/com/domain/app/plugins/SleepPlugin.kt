package com.domain.app.plugins

import android.content.Context
import com.domain.app.core.data.DataPoint
import com.domain.app.core.plugin.*
import com.domain.app.core.plugin.security.*
import java.time.LocalTime

/**
 * Sleep tracking plugin with security manifest
 */
class SleepPlugin : Plugin {
    override val id = "sleep"
    
    override val metadata = PluginMetadata(
        name = "Sleep",
        description = "Track your sleep duration, quality, and dreams",
        version = "1.0.0",
        author = "System",
        category = PluginCategory.HEALTH,
        tags = listOf("sleep", "rest", "health", "recovery", "dreams"),
        dataPattern = DataPattern.COMPOSITE,
        inputType = InputType.SLIDER,  // Changed to SLIDER for primary input
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
            id = "sleep_data",
            title = "Log Your Sleep",
            inputType = InputType.SLIDER,  // Custom dual slider
            required = true,
            hint = "Set duration and quality"
        ),
        QuickAddStage(
            id = "dream",
            title = "Remember any dreams?",
            inputType = InputType.TEXT,
            required = false,
            hint = "Optional dream journal"
        )
    )
    
    override fun getQuickAddConfig() = QuickAddConfig(
        title = "Log Sleep",
        inputType = InputType.SLIDER,
        defaultValue = mapOf("duration" to 480, "quality" to 50)  // 8 hours, 50% quality
    )
    
    override suspend fun createManualEntry(data: Map<String, Any>): DataPoint? {
        // Handle both old format (duration in minutes) and new format (map with duration and quality)
        val (durationMinutes, quality) = when (val sleepData = data["sleep_data"]) {
            is Map<*, *> -> {
                val duration = (sleepData["duration"] as? Number)?.toInt() ?: 480
                val qual = (sleepData["quality"] as? Number)?.toInt() ?: 50
                duration to qual
            }
            else -> {
                // Fallback for simple duration input
                val duration = (data["duration"] as? Number)?.toInt() ?: 
                              (data["amount"] as? Number)?.toInt() ?: 480
                val qual = (data["quality"] as? Number)?.toInt() ?: 50
                duration to qual
            }
        }
        
        val dream = data["dream"] as? String
        
        val hours = durationMinutes / 60
        val mins = durationMinutes % 60
        
        return DataPoint(
            pluginId = id,
            type = "sleep_session",
            value = mapOf(
                "duration_minutes" to durationMinutes,
                "hours" to hours,
                "minutes" to mins,
                "quality" to quality,  // Now 0-100 scale
                "quality_label" to getQualityLabel(quality),
                "dream_journal" to (dream ?: ""),
                "sleep_type" to categorizeSleep(durationMinutes)
            ),
            metadata = mapOf(
                "quick_add" to "true",
                "has_dream" to (dream != null).toString(),
                "time_of_day" to getTimeOfDay()
            ),
            source = "manual"
        )
    }
    
    override fun validateDataPoint(data: Map<String, Any>): ValidationResult {
        val sleepData = data["sleep_data"] as? Map<*, *>
        val duration = sleepData?.get("duration") as? Number ?: 
                      data["duration"] as? Number
        val quality = sleepData?.get("quality") as? Number ?: 
                     data["quality"] as? Number
        
        return when {
            duration == null -> ValidationResult.Error("Duration is required")
            duration.toInt() <= 0 -> ValidationResult.Error("Duration must be positive")
            duration.toInt() > 1440 -> ValidationResult.Warning("That's over 24 hours! Are you sure?")
            quality != null && quality.toInt() !in 0..100 -> ValidationResult.Error("Quality must be between 0 and 100")
            else -> ValidationResult.Success
        }
    }
    
    override fun exportHeaders() = listOf(
        "Date", "Time", "Duration (hours)", "Quality (0-100)", "Quality Label", "Dream Notes", "Sleep Type"
    )
    
    override fun formatForExport(dataPoint: DataPoint): Map<String, String> {
        val date = dataPoint.timestamp.toString().split("T")[0]
        val time = dataPoint.timestamp.toString().split("T")[1].split(".")[0]
        val hours = dataPoint.value["hours"]?.toString() ?: "0"
        val minutes = dataPoint.value["minutes"]?.toString() ?: "0"
        val duration = "$hours:${minutes.padStart(2, '0')}"
        
        return mapOf(
            "Date" to date,
            "Time" to time,
            "Duration (hours)" to duration,
            "Quality (0-100)" to (dataPoint.value["quality"]?.toString() ?: ""),
            "Quality Label" to (dataPoint.value["quality_label"]?.toString() ?: ""),
            "Dream Notes" to (dataPoint.value["dream_journal"]?.toString() ?: ""),
            "Sleep Type" to (dataPoint.value["sleep_type"]?.toString() ?: "")
        )
    }
    
    private fun getQualityLabel(quality: Int) = when {
        quality >= 80 -> "Excellent"
        quality >= 60 -> "Good"
        quality >= 40 -> "Fair"
        quality >= 20 -> "Poor"
        else -> "Terrible"
    }
    
    private fun categorizeSleep(minutes: Int) = when {
        minutes < 30 -> "Power Nap"
        minutes < 120 -> "Nap"
        minutes < 360 -> "Short Sleep"
        minutes < 540 -> "Normal Sleep"
        else -> "Long Sleep"
    }
    
    private fun getTimeOfDay(): String {
        val hour = LocalTime.now().hour
        return when (hour) {
            in 5..11 -> "morning"
            in 12..16 -> "afternoon"
            in 17..21 -> "evening"
            else -> "night"
        }
    }
}
