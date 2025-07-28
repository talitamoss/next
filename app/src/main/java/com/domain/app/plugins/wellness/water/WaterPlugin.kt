package com.domain.app.plugins.wellness.water

import android.content.Context
import com.domain.app.core.data.DataPoint
import com.domain.app.core.plugin.*

/**
 * Water tracking plugin - MVP implementation
 * Tracks water consumption with simple quick-add functionality
 */
class WaterPlugin : Plugin {
    override val id = "water"
    
    override val metadata = PluginMetadata(
        name = "Water Intake",
        description = "Track your daily water consumption",
        version = "1.0.0",
        author = "System",
        category = PluginCategory.HEALTH
    )
    
    override suspend fun initialize(context: Context) {
        // No special initialization needed for water tracking
        // Future: Could load user preferences, daily goals, etc.
    }
    
    override fun supportsManualEntry(): Boolean = true
    
    override fun getQuickAddConfig(): QuickAddConfig {
        return QuickAddConfig(
            title = "Add Water",
            defaultValue = 250,
            inputType = InputType.CHOICE,
            options = listOf(
                QuickOption("Glass", 250, "☕"),
                QuickOption("Bottle", 500, "🍶"),
                QuickOption("Liter", 1000, "💧"),
                QuickOption("Custom", -1, "✏️")
            ),
            unit = "ml"
        )
    }
    
    override suspend fun createManualEntry(data: Map<String, Any>): DataPoint? {
        // Extract amount from input data
        val amount = when (val value = data["amount"]) {
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull() ?: return null
            else -> return null
        }
        
        // Validate amount
        if (amount <= 0 || amount > 5000) {
            return null // Invalid amount
        }
        
        // Get unit (default to ml)
        val unit = data["unit"] as? String ?: "ml"
        
        // Convert to ml if needed
        val amountInMl = when (unit) {
            "ml" -> amount
            "l", "L" -> amount * 1000
            "oz" -> amount * 29.5735
            "cups" -> amount * 236.588
            else -> amount
        }
        
        // Create the data point
        return DataPoint(
            pluginId = id,
            type = "water_intake",
            value = mapOf(
                "amount" to amountInMl,
                "unit" to "ml",
                "original_amount" to amount,
                "original_unit" to unit
            ),
            metadata = mapOf(
                "quick_add" to "true",
                "time_of_day" to getTimeOfDay()
            ),
            source = "manual"
        )
    }
    
    /**
     * Helper function to categorize time of day
     */
    private fun getTimeOfDay(): String {
        val hour = java.time.LocalTime.now().hour
        return when (hour) {
            in 5..11 -> "morning"
            in 12..16 -> "afternoon"
            in 17..21 -> "evening"
            else -> "night"
        }
    }
    
    /**
     * Future expansion example - these methods could be added later
     * without breaking existing code
     */
    
    // Future: Daily goal tracking
    fun getDailyGoal(): Double = 2000.0 // ml
    
    // Future: Calculate today's progress
    suspend fun getTodayProgress(dataPoints: List<DataPoint>): Double {
        val today = java.time.LocalDate.now()
        val todayIntake = dataPoints
            .filter { 
                it.pluginId == id && 
                it.timestamp.atZone(java.time.ZoneId.systemDefault()).toLocalDate() == today 
            }
            .sumOf { (it.value["amount"] as? Number)?.toDouble() ?: 0.0 }
        
        return (todayIntake / getDailyGoal()) * 100
    }
    
    // Future: Smart reminders
    fun shouldRemindUser(lastIntake: DataPoint?): Boolean {
        if (lastIntake == null) return true
        
        val hoursSinceLastIntake = java.time.Duration
            .between(lastIntake.timestamp, java.time.Instant.now())
            .toHours()
            
        return hoursSinceLastIntake >= 2
    }
}
