package com.domain.app.plugins.wellness.exercise

import android.content.Context
import com.domain.app.core.data.DataPoint
import com.domain.app.core.plugin.*

class ExercisePlugin : Plugin {
    override val id = "exercise"
    
    override val metadata = PluginMetadata(
        name = "Exercise",
        description = "Log workouts and physical activities",
        version = "1.0.0",
        author = "System",
        category = PluginCategory.HEALTH
    )
    
    override suspend fun initialize(context: Context) {
        // No special initialization needed
    }
    
    override fun supportsManualEntry() = true
    
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
            type = "exercise_log",
            value = mapOf(
                "activity" to activityType,
                "duration_minutes" to duration,
                "intensity" to intensity,
                "calories" to (calories ?: estimateCalories(activityType, duration, intensity)),
                "notes" to (notes ?: ""),
                "emoji" to getEmojiForActivity(activityType)
            ),
            metadata = mapOf(
                "quick_add" to "true"
            ),
            source = "manual"
        )
    }
    
    private fun getEmojiForActivity(activity: String) = when(activity) {
        "walk" -> "🚶"
        "run" -> "🏃"
        "gym" -> "💪"
        "yoga" -> "🧘"
        "bike" -> "🚴"
        else -> "🏋️"
    }
    
    private fun estimateCalories(activity: String, duration: Int, intensity: String): Int {
        // Simple estimation - in production would use user weight/age
        val baseRate = when(activity) {
            "walk" -> 4
            "run" -> 10
            "gym" -> 8
            "yoga" -> 3
            "bike" -> 7
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
