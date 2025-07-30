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
            required = true,
            hint = "Enter hours and minutes"
        ),
        QuickAddStage(
            id = "quality",
            title = "Rate your sleep quality",
            inputType = InputType.SCALE,
            required = true,
            options = (1..5).map { 
                QuickOption(
                    label = getQualityLabel(it),
                    value = it,
                    icon = getQualityEmoji(it)
                )
            }
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
        inputType = InputType.CHOICE,
        options = listOf(
            QuickOption("Quick Nap", 30, "😴"),
            QuickOption("Power Nap", 90, "💤"),
            QuickOption("Full Night", 480, "🛏️"),
            QuickOption("Custom", -1, "⏰")
        ),
        unit = "minutes"
    )
    
    override suspend fun createManualEntry(data: Map<String, Any>): DataPoint? {
        val minutes = when (val value = data["duration"] ?: data["amount"]) {
            is Number -> value.toInt()
            else -> return null
        }
        
        val quality = (data["quality"] as? Number)?.toInt() ?: 3
        val dream = data["dream"] as? String
        
        val hours = minutes / 60
        val mins = minutes % 60
        
        return DataPoint(
            pluginId = id,
            type = "sleep_session",
            value = mapOf(
                "duration_minutes" to minutes,
                "hours" to hours,
                "minutes" to mins,
                "quality" to quality,
                "quality_label" to getQualityLabel(quality),
                "dream_journal" to (dream ?: ""),
                "sleep_type" to categorizeSleep(minutes)
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
        val duration = (data["duration"] as? Number)?.toInt()
        val quality = (data["quality"] as? Number)?.toInt()
        
        return when {
            duration == null -> ValidationResult.Error("Duration is required")
            duration <= 0 -> ValidationResult.Error("Duration must be positive")
            duration > 1440 -> ValidationResult.Warning("That's over 24 hours! Are you sure?")
            quality != null && quality !in 1..5 -> ValidationResult.Error("Quality must be between 1 and 5")
            else -> ValidationResult.Success
        }
    }
    
    override fun exportHeaders() = listOf(
        "Date", "Time", "Duration (hours)", "Quality (1-5)", "Quality Label", "Dream Notes", "Sleep Type"
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
            "Quality (1-5)" to (dataPoint.value["quality"]?.toString() ?: ""),
            "Quality Label" to (dataPoint.value["quality_label"]?.toString() ?: ""),
            "Dream Notes" to (dataPoint.value["dream_journal"]?.toString() ?: ""),
            "Sleep Type" to (dataPoint.value["sleep_type"]?.toString() ?: "")
        )
    }
    
    private fun getQualityLabel(quality: Int) = when(quality) {
        5 -> "Excellent"
        4 -> "Good"
        3 -> "Fair"
        2 -> "Poor"
        1 -> "Terrible"
        else -> "Unknown"
    }
    
    private fun getQualityEmoji(quality: Int) = when(quality) {
        5 -> "😊"
        4 -> "🙂"
        3 -> "😐"
        2 -> "😕"
        1 -> "😫"
        else -> "😐"
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
