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
    
    override val metadata = PluginMetadata(
        name = "Exercise",
        description = "Log workouts and physical activities",
        version = "1.0.0",
        author = "System",
        category = PluginCategory.HEALTH,
        tags = listOf("exercise", "fitness", "workout", "activity", "health"),
        dataPattern = DataPattern.COMPOSITE,
        inputType = InputType.CHOICE,
        supportsMultiStage = true,
        relatedPlugins = listOf("water", "energy", "mood", "sleep"),
        exportFormat = ExportFormat.CSV,
        dataSensitivity = DataSensitivity.NORMAL,
        naturalLanguageAliases = listOf(
            "exercise", "workout", "gym", "run", "ran", "running",
            "walk", "walked", "walking", "bike", "biked", "cycling",
            "yoga", "swim", "swimming", "sport", "training",
            "cardio", "weights", "fitness"
        ),
        contextualTriggers = listOf(
            ContextTrigger.TIME_OF_DAY,
            ContextTrigger.LOCATION,
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
        dataSensitivity = DataSensitivity.NORMAL,
        dataAccess = setOf(DataAccessScope.OWN_DATA_ONLY),
        privacyPolicy = "Exercise data is stored locally and used to track your fitness progress. Location data is not collected unless explicitly enabled.",
        dataRetention = DataRetentionPolicy.USER_CONTROLLED
    )
    
    override val trustLevel = PluginTrustLevel.OFFICIAL
    
    override fun getPermissionRationale() = mapOf(
        PluginCapability.COLLECT_DATA to "Record your workouts and physical activities",
        PluginCapability.READ_OWN_DATA to "View your exercise history and progress",
        PluginCapability.LOCAL_STORAGE to "Save your workout data on your device",
        PluginCapability.EXPORT_DATA to "Export fitness data for analysis or sharing",
        PluginCapability.ANALYTICS_BASIC to "Calculate calories burned and activity trends"
    )
    
    override suspend fun initialize(context: Context) {
        // No special initialization needed
    }
    
    override fun supportsManualEntry() = true
    
    override fun getQuickAddStages() = listOf(
        QuickAddStage(
            id = "activity",
            title = "What activity did you do?",
            inputType = InputType.CHOICE,
            required = true,
            options = listOf(
                QuickOption("Walk", "walk", "🚶"),
                QuickOption("Run", "run", "🏃"),
                QuickOption("Gym", "gym", "💪"),
                QuickOption("Yoga", "yoga", "🧘"),
                QuickOption("Bike", "bike", "🚴"),
                QuickOption("Swim", "swim", "🏊"),
                QuickOption("Sports", "sports", "⚽"),
                QuickOption("Other", "other", "🏋️")
            )
        ),
        QuickAddStage(
            id = "duration",
            title = "How long did you exercise?",
            inputType = InputType.NUMBER,
            required = true,
            hint = "Duration in minutes",
            defaultValue = 30
        ),
        QuickAddStage(
            id = "intensity",
            title = "How intense was it?",
            inputType = InputType.CHOICE,
            required = true,
            options = listOf(
                QuickOption("Light", "light", "😌"),
                QuickOption("Moderate", "moderate", "😊"),
                QuickOption("Intense", "intense", "😤")
            )
        ),
        QuickAddStage(
            id = "notes",
            title = "Any notes?",
            inputType = InputType.TEXT,
            required = false,
            hint = "Optional notes about your workout"
        )
    )
    
    override fun getQuickAddConfig() = QuickAddConfig(
        title = "Log Exercise",
        inputType = InputType.CHOICE,
        options = listOf(
            QuickOption("Walk", "walk", "🚶"),
            QuickOption("Run", "run", "🏃"),
            QuickOption("Gym", "gym", "💪"),
            QuickOption("Yoga", "yoga", "🧘"),
            QuickOption("Bike", "bike", "🚴"),
            QuickOption("Other", "other", "🏋️")
        )
    )
    
    override suspend fun createManualEntry(data: Map<String, Any>): DataPoint? {
        val activityType = data["activity"] as? String ?: data["value"] as? String ?: "other"
        val duration = (data["duration"] as? Number)?.toInt() ?: 30
        val intensity = data["intensity"] as? String ?: "moderate"
        val calories = data["calories"] as? Int
        val notes = data["notes"] as? String
        
        return DataPoint(
            pluginId = id,
            type = "exercise_session",
            value = mapOf(
                "activity" to activityType,
                "activity_label" to getActivityLabel(activityType),
                "duration_minutes" to duration,
                "intensity" to intensity,
                "calories" to (calories ?: estimateCalories(activityType, duration, intensity)),
                "notes" to (notes ?: ""),
                "emoji" to getEmojiForActivity(activityType)
            ),
            metadata = mapOf(
                "quick_add" to "true",
                "has_notes" to (notes != null).toString()
            ),
            source = "manual"
        )
    }
    
    override fun validateDataPoint(data: Map<String, Any>): ValidationResult {
        val duration = (data["duration"] as? Number)?.toInt()
        val activity = data["activity"] as? String
        
        return when {
            activity.isNullOrBlank() -> ValidationResult.Error("Activity type is required")
            duration == null -> ValidationResult.Error("Duration is required")
            duration <= 0 -> ValidationResult.Error("Duration must be positive")
            duration > 720 -> ValidationResult.Warning("That's over 12 hours! Are you sure?")
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
            "Activity" to (dataPoint.value["activity_label"]?.toString() ?: ""),
            "Duration (min)" to (dataPoint.value["duration_minutes"]?.toString() ?: ""),
            "Intensity" to (dataPoint.value["intensity"]?.toString() ?: ""),
            "Calories" to (dataPoint.value["calories"]?.toString() ?: ""),
            "Notes" to (dataPoint.value["notes"]?.toString() ?: "")
        )
    }
    
    private fun getActivityLabel(activity: String) = when(activity) {
        "walk" -> "Walking"
        "run" -> "Running"
        "gym" -> "Gym Workout"
        "yoga" -> "Yoga"
        "bike" -> "Cycling"
        "swim" -> "Swimming"
        "sports" -> "Sports"
        else -> "Other Exercise"
    }
    
    private fun getEmojiForActivity(activity: String) = when(activity) {
        "walk" -> "🚶"
        "run" -> "🏃"
        "gym" -> "💪"
        "yoga" -> "🧘"
        "bike" -> "🚴"
        "swim" -> "🏊"
        "sports" -> "⚽"
        else -> "🏋️"
    }
    
    private fun estimateCalories(activity: String, duration: Int, intensity: String): Int {
        val baseRate = when(activity) {
            "walk" -> 4
            "run" -> 10
            "gym" -> 8
            "yoga" -> 3
            "bike" -> 7
            "swim" -> 9
            "sports" -> 7
            else -> 5
        }
        
        val intensityMultiplier = when(intensity) {
            "light" -> 0.8
            "moderate" -> 1.0
            "intense" -> 1.5
            else -> 1.0
        }
        
        return (baseRate * duration * intensityMultiplier).toInt()
    }
}
