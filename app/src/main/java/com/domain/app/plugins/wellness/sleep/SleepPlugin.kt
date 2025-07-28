package com.domain.app.plugins.wellness.sleep

import android.content.Context
import com.domain.app.core.data.DataPoint
import com.domain.app.core.plugin.*
import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class SleepPlugin : Plugin {
    override val id = "sleep"
    
    override val metadata = PluginMetadata(
        name = "Sleep",
        description = "Track your sleep duration and quality",
        version = "1.0.0",
        author = "System",
        category = PluginCategory.HEALTH
    )
    
    override suspend fun initialize(context: Context) {
        // No special initialization needed
    }
    
    override fun supportsManualEntry() = true
    
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
        
        val quality = data["quality"] as? Int ?: 3
        val notes = data["notes"] as? String
        
        // Calculate hours and minutes for display
        val hours = minutes / 60
        val mins = minutes % 60
        
        return DataPoint(
            pluginId = id,
            type = "sleep_log",
            value = mapOf(
                "duration_minutes" to minutes,
                "hours" to hours,
                "minutes" to mins,
                "quality" to quality,
                "notes" to (notes ?: ""),
                "time_of_day" to getTimeOfDay()
            ),
            metadata = mapOf(
                "quick_add" to "true"
            ),
            source = "manual"
        )
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
