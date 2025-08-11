package com.domain.app.plugins
import com.domain.app.core.validation.ValidationResult

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
                // Cardio
                QuickOption("Running", "running", null),
                QuickOption("Walking", "walking", null),
                QuickOption("Cycling", "cycling", null),
                QuickOption("Swimming", "swimming", null),
                QuickOption("Hiking", "hiking", null),
                QuickOption("Rowing", "rowing", null),
                QuickOption("Elliptical", "elliptical", null),
                
                // Strength
                QuickOption("Weight Training", "weights", null),
                QuickOption("Bodyweight", "bodyweight", null),
                QuickOption("CrossFit", "crossfit", null),
                
                // Flexibility & Balance
                QuickOption("Yoga", "yoga", null),
                QuickOption("Pilates", "pilates", null),
                QuickOption("Stretching", "stretching", null),
                
                // Sports
                QuickOption("Tennis", "tennis", null),
                QuickOption("Basketball", "basketball", null),
                QuickOption("Soccer", "soccer", null),
                QuickOption("Golf", "golf", null),
                QuickOption("Martial Arts", "martial_arts", null),
                
                // Dance & Movement
                QuickOption("Dancing", "dancing", null),
                QuickOption("Boxing", "boxing", null),
                
                // Other
                QuickOption("Rock Climbing", "climbing", null),
                QuickOption("Other", "other", null)
            )
        ),
        QuickAddStage(
            id = "duration",
            title = "How long did you exercise?",
            inputType = InputType.SLIDER,  // Changed to SLIDER
            required = true,
            hint = "Slide to set duration",
            defaultValue = 30
        ),
        QuickAddStage(
            id = "intensity",
            title = "How intense was it?",
            inputType = InputType.CHOICE,
            required = true,
            options = listOf(
                QuickOption("Light", "light", null),
                QuickOption("Moderate", "moderate", null),
                QuickOption("Intense", "intense", null)
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
            QuickOption("Running", "running", null),
            QuickOption("Walking", "walking", null),
            QuickOption("Gym", "gym", null),
            QuickOption("Yoga", "yoga", null),
            QuickOption("Cycling", "cycling", null),
            QuickOption("Other", "other", null)
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
                "notes" to (notes ?: "")
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
        "running" -> "Running"
        "walking" -> "Walking"
        "cycling" -> "Cycling"
        "swimming" -> "Swimming"
        "hiking" -> "Hiking"
        "rowing" -> "Rowing"
        "elliptical" -> "Elliptical"
        "weights" -> "Weight Training"
        "bodyweight" -> "Bodyweight"
        "crossfit" -> "CrossFit"
        "yoga" -> "Yoga"
        "pilates" -> "Pilates"
        "stretching" -> "Stretching"
        "tennis" -> "Tennis"
        "basketball" -> "Basketball"
        "soccer" -> "Soccer"
        "golf" -> "Golf"
        "martial_arts" -> "Martial Arts"
        "dancing" -> "Dancing"
        "boxing" -> "Boxing"
        "climbing" -> "Rock Climbing"
        "gym" -> "Gym Workout"
        else -> "Other Exercise"
    }
    
    private fun estimateCalories(activity: String, duration: Int, intensity: String): Int {
        // Calories per minute at moderate intensity (rough estimates)
        val baseRate = when(activity) {
            "running" -> 10
            "walking" -> 4
            "cycling" -> 7
            "swimming" -> 9
            "hiking" -> 6
            "rowing" -> 8
            "elliptical" -> 7
            "weights" -> 6
            "bodyweight" -> 7
            "crossfit" -> 10
            "yoga" -> 3
            "pilates" -> 4
            "stretching" -> 2
            "tennis" -> 7
            "basketball" -> 8
            "soccer" -> 8
            "golf" -> 3
            "martial_arts" -> 9
            "dancing" -> 5
            "boxing" -> 9
            "climbing" -> 10
            else -> 5
        }
        
        val intensityMultiplier = when(intensity) {
            "light" -> 0.7
            "moderate" -> 1.0
            "intense" -> 1.4
            else -> 1.0
        }
        
        return (baseRate * duration * intensityMultiplier).toInt()
    }
}
