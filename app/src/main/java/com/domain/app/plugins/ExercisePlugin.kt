package com.domain.app.plugins

import android.content.Context
import com.domain.app.core.data.DataPoint
import com.domain.app.core.plugin.*
import com.domain.app.core.plugin.security.*

/**
 * Exercise tracking plugin with security manifest
 */
class ExercisePlugin : Plugin {
    override val id = "exercise"
    
    override val supportsAutomaticCollection = false

    override val metadata = PluginMetadata(
        name = "Exercise",
        description = "Track your physical activities and workouts",
        version = "1.0.0",
        author = "System",
        category = PluginCategory.FITNESS,
        tags = listOf("exercise", "fitness", "workout", "activity", "health"),
        dataPattern = DataPattern.COMPOSITE,
        inputType = InputType.CHOICE,
        supportsMultiStage = true,
        relatedPlugins = listOf("water", "mood", "sleep", "energy"),
        exportFormat = ExportFormat.CSV,
        dataSensitivity = DataSensitivity.NORMAL,
        naturalLanguageAliases = listOf(
            "exercise", "workout", "gym", "run", "ran",
            "walk", "walked", "bike", "swim", "yoga",
            "lift", "cardio", "training", "sport"
        ),
        contextualTriggers = listOf(
            ContextTrigger.TIME_OF_DAY,
            ContextTrigger.LOCATION_BASED
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
        dataSensitivity = DataSensitivity.NORMAL,
        dataAccess = setOf(DataAccessScope.OWN_DATA_ONLY),
        privacyPolicy = "Exercise data is stored locally and can be exported for fitness tracking. Location data is never collected.",
        dataRetention = DataRetentionPolicy.USER_CONTROLLED
    )
    
    override val trustLevel = PluginTrustLevel.OFFICIAL
    
    override fun getPermissionRationale() = mapOf(
        PluginCapability.COLLECT_DATA to "Record your exercise activities",
        PluginCapability.READ_OWN_DATA to "View your workout history and progress",
        PluginCapability.LOCAL_STORAGE to "Save your exercise data on your device",
        PluginCapability.EXPORT_DATA to "Export workout data for fitness analysis",
        PluginCapability.ANALYTICS_BASIC to "Calculate exercise trends and statistics"
    )
    
    override suspend fun initialize(context: Context) {
        // No special initialization needed
    }
    
    override fun supportsManualEntry() = true
    
    override fun getQuickAddStages() = listOf(
        QuickAddStage(
            id = "activity",
            title = "What activity?",
            inputType = InputType.CHOICE,
            required = true,
            options = listOf(
                QuickOption("Running", "running", "🏃"),
                QuickOption("Walking", "walking", "🚶"),
                QuickOption("Cycling", "cycling", "🚴"),
                QuickOption("Swimming", "swimming", "🏊"),
                QuickOption("Gym", "gym", "🏋️"),
                QuickOption("Yoga", "yoga", "🧘"),
                QuickOption("Sports", "sports", "⚽"),
                QuickOption("Other", "other", "💪")
            )
        ),
        QuickAddStage(
            id = "duration",
            title = "How long?",
            inputType = InputType.DURATION,
            required = true
        ),
        QuickAddStage(
            id = "intensity",
            title = "How intense?",
            inputType = InputType.CHOICE,
            required = true,
            options = listOf(
                QuickOption("Light", 1, "🌤️"),
                QuickOption("Moderate", 2, "☀️"),
                QuickOption("Vigorous", 3, "🔥")
            )
        ),
        QuickAddStage(
            id = "notes",
            title = "Any notes?",
            inputType = InputType.TEXT,
            required = false,
            hint = "Distance, sets, etc."
        )
    )
    
    override fun getQuickAddConfig() = QuickAddConfig(
        title = "Quick Exercise Log",
        inputType = InputType.CHOICE,
        options = listOf(
            QuickOption("30 min walk", mapOf("activity" to "walking", "duration" to 30), "🚶"),
            QuickOption("Gym session", mapOf("activity" to "gym", "duration" to 60), "🏋️"),
            QuickOption("Morning run", mapOf("activity" to "running", "duration" to 30), "🏃"),
            QuickOption("Custom", -1, "➕")
        )
    )
    
    override suspend fun createManualEntry(data: Map<String, Any>): DataPoint? {
        val activity = data["activity"] as? String ?: "other"
        val duration = when (val value = data["duration"]) {
            is Number -> value.toLong()
            is String -> parseDuration(value)
            else -> null
        } ?: return null
        
        val intensity = (data["intensity"] as? Number)?.toInt() ?: 2
        val notes = data["notes"] as? String
        
        return DataPoint(
            pluginId = id,
            type = "exercise_log",
            value = mapOf(
                "activity" to activity,
                "activity_emoji" to getActivityEmoji(activity),
                "duration_minutes" to duration,
                "intensity" to intensity,
                "intensity_label" to getIntensityLabel(intensity),
                "notes" to (notes ?: ""),
                "calories_estimate" to estimateCalories(activity, duration, intensity)
            ),
            metadata = mapOf(
                "quick_add" to "true",
                "time_of_day" to getTimeOfDay()
            ),
            source = "manual"
        )
    }
    
    override fun validateDataPoint(data: Map<String, Any>): ValidationResult {
        val duration = (data["duration"] as? Number)?.toLong()
        val intensity = (data["intensity"] as? Number)?.toInt()
        
        return when {
            duration == null -> ValidationResult.Error("Duration is required")
            duration <= 0 -> ValidationResult.Error("Duration must be positive")
            duration > 480 -> ValidationResult.Warning("That's over 8 hours! Are you sure?")
            intensity != null && intensity !in 1..3 -> ValidationResult.Error("Intensity must be between 1 and 3")
            else -> ValidationResult.Success
        }
    }
    
    override fun exportHeaders() = listOf(
        "Date", "Time", "Activity", "Duration (min)", "Intensity", "Calories", "Notes"
    )
    
    override fun formatForExport(dataPoint: DataPoint): Map<String, String> {
        val date = dataPoint.timestamp.toString().split("T")[0]
        val time = dataPoint.timestamp.toString().split("T")[1].split(".")[0]
        
        return mapOf(
            "Date" to date,
            "Time" to time,
            "Activity" to (dataPoint.value["activity"]?.toString() ?: ""),
            "Duration (min)" to (dataPoint.value["duration_minutes"]?.toString() ?: ""),
            "Intensity" to (dataPoint.value["intensity_label"]?.toString() ?: ""),
            "Calories" to (dataPoint.value["calories_estimate"]?.toString() ?: ""),
            "Notes" to (dataPoint.value["notes"]?.toString() ?: "")
        )
    }
    
    private fun parseDuration(value: String): Long? {
        // Parse formats like "30m", "1h", "1h30m"
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
    
    private fun getActivityEmoji(activity: String): String = when (activity) {
        "running" -> "🏃"
        "walking" -> "🚶"
        "cycling" -> "🚴"
        "swimming" -> "🏊"
        "gym" -> "🏋️"
        "yoga" -> "🧘"
        "sports" -> "⚽"
        else -> "💪"
    }
    
    private fun getIntensityLabel(intensity: Int): String = when (intensity) {
        1 -> "Light"
        2 -> "Moderate"
        3 -> "Vigorous"
        else -> "Unknown"
    }
    
    private fun estimateCalories(activity: String, duration: Long, intensity: Int): Int {
        // Very rough estimates
        val baseRate = when (activity) {
            "running" -> 10
            "cycling" -> 8
            "swimming" -> 11
            "walking" -> 4
            "gym" -> 7
            "yoga" -> 3
            "sports" -> 8
            else -> 5
        }
        
        val intensityMultiplier = when (intensity) {
            1 -> 0.7
            2 -> 1.0
            3 -> 1.4
            else -> 1.0
        }
        
        return (baseRate * duration * intensityMultiplier / 60).toInt()
    }
    
    private fun getTimeOfDay(): String {
        val hour = java.time.LocalTime.now().hour
        return when (hour) {
            in 5..11 -> "morning"
            in 12..16 -> "afternoon"
            in 17..21 -> "evening"
            else -> "night"
        }
    }
}
