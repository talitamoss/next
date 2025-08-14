// app/src/main/java/com/domain/app/plugins/ExercisePlugin.kt
package com.domain.app.plugins

import android.content.Context
import com.domain.app.core.data.DataPoint
import com.domain.app.core.plugin.*
import com.domain.app.core.plugin.security.*
import com.domain.app.core.validation.ValidationResult

/**
 * Exercise tracking plugin with composite inputs
 */
class ExercisePlugin : Plugin {
    override val id = "exercise"
    
    override val metadata = PluginMetadata(
        name = "Exercise",
        description = "Track your physical activities",
        version = "1.0.0",
        author = "System",
        category = PluginCategory.HEALTH,
        tags = listOf("exercise", "fitness", "workout", "activity"),
        dataPattern = DataPattern.COMPOSITE,
        inputType = InputType.NUMBER, // Default, but we use inputs instead
        supportsMultiStage = false,   // Using composite instead
        relatedPlugins = listOf("mood", "sleep", "water"),
        exportFormat = ExportFormat.CSV,
        dataSensitivity = DataSensitivity.NORMAL,
        naturalLanguageAliases = listOf(
            "exercise", "workout", "training", "run", "walk", "cycle",
            "I exercised", "went for a run", "worked out", "gym session"
        ),
        contextualTriggers = listOf(
            ContextTrigger.TIME_OF_DAY,
            ContextTrigger.LOCATION
        )
    )
    
    override val securityManifest = PluginSecurityManifest(
        requestedCapabilities = setOf(
            PluginCapability.COLLECT_DATA,
            PluginCapability.READ_OWN_DATA,
            PluginCapability.LOCAL_STORAGE,
            PluginCapability.EXPORT_DATA
        ),
        dataSensitivity = DataSensitivity.NORMAL,
        dataAccess = setOf(DataAccessScope.OWN_DATA_ONLY),
        privacyPolicy = "Exercise data is stored locally and encrypted. We never share this data without your explicit consent.",
        dataRetention = DataRetentionPolicy.USER_CONTROLLED
    )
    
    override val trustLevel = PluginTrustLevel.OFFICIAL
    
    override fun getPermissionRationale() = mapOf(
        PluginCapability.COLLECT_DATA to "Record your exercise activities",
        PluginCapability.READ_OWN_DATA to "View your exercise history",
        PluginCapability.LOCAL_STORAGE to "Save your workout data on your device",
        PluginCapability.EXPORT_DATA to "Export your fitness data for analysis"
    )
    
    override suspend fun initialize(context: Context) {
        // No special initialization needed
    }
    
    override fun supportsManualEntry() = true
    
    override fun getQuickAddConfig() = QuickAddConfig(
        title = "Track Exercise",
        inputs = listOf(
            QuickAddInput(
                id = "type",
                label = "Exercise",
                type = InputType.CAROUSEL,
                options = listOf(
                    QuickOption("Walking", "walking"),
                    QuickOption("Running", "running"),
                    QuickOption("Cycling", "cycling"),
                    QuickOption("Swimming", "swimming"),
                    QuickOption("Gym", "gym"),
                    QuickOption("Yoga", "yoga"),
                    QuickOption("Hiking", "hiking")
                ),
                defaultValue = "running"
            ),
            QuickAddInput(
                id = "distance",
                label = "Distance",
                type = InputType.HORIZONTAL_SLIDER,
                min = 0,
                max = 10,
                unit = "km",
                defaultValue = 5.0f,
                topLabel = "0",
                bottomLabel = "10 km"
            ),
            QuickAddInput(
                id = "intensity",
                label = "Intensity",
                type = InputType.HORIZONTAL_SLIDER,
                min = 1,
                max = 5,
                defaultValue = 3.0f,
                topLabel = "Easy",
                bottomLabel = "Hard"
            )
        )
    )
    
    override suspend fun createManualEntry(data: Map<String, Any>): DataPoint? {
        val exerciseType = data["type"] as? String ?: return null
        val distance = (data["distance"] as? Number)?.toFloat() ?: 0f
        val intensity = (data["intensity"] as? Number)?.toInt() ?: 3
        
        val validationResult = validateDataPoint(data)
        if (validationResult is ValidationResult.Error) {
            return null
        }
        
        return DataPoint(
            pluginId = id,
            type = "exercise_session",
            value = mapOf(
                "type" to exerciseType,
                "distance" to distance,
                "intensity" to intensity,
                "intensity_label" to getIntensityLabel(intensity)
            ),
            metadata = mapOf(
                "quick_add" to "true"
            ),
            source = "manual"
        )
    }
    
    override fun validateDataPoint(data: Map<String, Any>): ValidationResult {
        val exerciseType = data["type"] as? String
        val distance = (data["distance"] as? Number)?.toFloat()
        val intensity = (data["intensity"] as? Number)?.toInt()
        
        return when {
            exerciseType.isNullOrBlank() -> ValidationResult.Error("Exercise type is required")
            distance == null || distance < 0 -> ValidationResult.Error("Distance must be a positive number")
            intensity == null || intensity !in 1..5 -> ValidationResult.Error("Intensity must be between 1 and 5")
            else -> ValidationResult.Success
        }
    }
    
    override fun exportHeaders() = listOf(
        "Date", "Time", "Type", "Distance", "Intensity", "IntensityLabel"
    )
    
    override fun formatForExport(dataPoint: DataPoint): Map<String, String> {
        val date = dataPoint.timestamp.toString().split("T")[0]
        val time = dataPoint.timestamp.toString().split("T")[1].split(".")[0]
        
        return mapOf(
            "Date" to date,
            "Time" to time,
            "Type" to (dataPoint.value["type"]?.toString() ?: ""),
            "Distance" to (dataPoint.value["distance"]?.toString() ?: "0"),
            "Intensity" to (dataPoint.value["intensity"]?.toString() ?: ""),
            "IntensityLabel" to (dataPoint.value["intensity_label"]?.toString() ?: "")
        )
    }
    
    private fun getIntensityLabel(value: Int): String {
        return when (value) {
            1 -> "Easy"
            2 -> "Light"
            3 -> "Moderate"
            4 -> "Vigorous"
            5 -> "Hard"
            else -> "Unknown"
        }
    }
}
