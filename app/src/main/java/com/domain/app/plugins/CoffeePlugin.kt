package com.domain.app.plugins
import com.domain.app.core.validation.ValidationResult

import android.content.Context
import com.domain.app.core.data.DataPoint
import com.domain.app.core.plugin.*
import com.domain.app.core.plugin.security.*

/**
 * Coffee intake tracking plugin for daily caffeine consumption monitoring
 */
class CoffeePlugin : Plugin {
    override val id = "coffee"
    
    override val metadata = PluginMetadata(
        name = "Coffee Intake",
        description = "Track your daily coffee consumption and caffeine intake",
        version = "1.0.0",
        author = "System",
        category = PluginCategory.HEALTH,
        tags = listOf("coffee", "caffeine", "beverage", "health", "daily", "intake"),
        dataPattern = DataPattern.CUMULATIVE,
        inputType = InputType.CHOICE,
        supportsMultiStage = false,
        relatedPlugins = listOf("water", "mood", "sleep"),
        exportFormat = ExportFormat.CSV,
        dataSensitivity = DataSensitivity.NORMAL,
        naturalLanguageAliases = listOf(
            "coffee", "caffeine", "espresso", "cappuccino", "latte",
            "americano", "macchiato", "mocha", "java", "brew",
            "cup of coffee", "coffee cup", "morning coffee", 
            "afternoon coffee", "coffee break", "had coffee"
        ),
        contextualTriggers = listOf(
            ContextTrigger.TIME_OF_DAY,
            ContextTrigger.AFTER_EVENT
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
        privacyPolicy = "Coffee intake data is stored locally and never shared without your permission.",
        dataRetention = DataRetentionPolicy.USER_CONTROLLED
    )
    
    override val trustLevel = PluginTrustLevel.OFFICIAL
    
    override fun getPermissionRationale() = mapOf(
        PluginCapability.COLLECT_DATA to "Record your daily coffee and caffeine intake",
        PluginCapability.READ_OWN_DATA to "View your coffee consumption history and patterns",
        PluginCapability.LOCAL_STORAGE to "Save your coffee intake data securely on your device",
        PluginCapability.EXPORT_DATA to "Export your coffee consumption data for personal analysis"
    )
    
    override suspend fun initialize(context: Context) {
        // No special initialization needed
    }
    
    override fun supportsManualEntry() = true
    
    override fun getQuickAddConfig() = QuickAddConfig(
        title = "Add Coffee",
        defaultValue = 1,
        inputType = InputType.CHOICE,
        options = listOf(
            QuickOption("Espresso Shot", 1, "☕"),
            QuickOption("Small Cup", 1, "☕"),
            QuickOption("Large Cup", 2, "☕"),
            QuickOption("Cappuccino", 1, "☕"),
            QuickOption("Latte", 1, "☕"),
            QuickOption("Americano", 1, "☕"),
            QuickOption("Cold Brew", 1, "🧊"),
            QuickOption("Custom", -1, "☕")
        ),
        unit = "cups"
    )
    
    override suspend fun createManualEntry(data: Map<String, Any>): DataPoint? {
        // Support both "value" (from GenericQuickAddDialog) and "amount" (from specific dialogs)
        val amount = when (val value = data["value"] ?: data["amount"]) {
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull() ?: return null
            else -> return null
        }
        
        val validationResult = validateDataPoint(mapOf("value" to amount))
        if (validationResult is ValidationResult.Error) {
            return null
        }
        
        val coffeeType = data["type"] as? String ?: "coffee"
        val caffeineContent = estimateCaffeineContent(amount, coffeeType)
        
        return DataPoint(
            pluginId = id,
            type = "coffee_intake",
            value = mapOf(
                "amount" to amount,
                "unit" to "cups",
                "coffee_type" to coffeeType,
                "caffeine_mg" to caffeineContent
            ),
            metadata = mapOf(
                "quick_add" to "true",
                "time_of_day" to getTimeOfDay(),
                "estimated_caffeine" to caffeineContent.toString()
            ),
            source = "manual"
        )
    }
    
    override fun validateDataPoint(data: Map<String, Any>): ValidationResult {
        // Support both "value" and "amount" keys for validation
        val amount = ((data["value"] ?: data["amount"]) as? Number)?.toDouble() 
            ?: return ValidationResult.Error("Amount is required")
        
        return when {
            amount <= 0 -> ValidationResult.Error("Amount must be positive")
            amount > 10 -> ValidationResult.Warning("That's over 10 cups of coffee! Consider your caffeine intake.")
            amount > 6 -> ValidationResult.Warning("High caffeine intake detected. Consider moderating consumption.")
            else -> ValidationResult.Success
        }
    }
    
    override fun exportHeaders() = listOf(
        "Date", "Time", "Amount (cups)", "Coffee Type", "Caffeine (mg)", "Time of Day"
    )
    
    override fun formatForExport(dataPoint: DataPoint): Map<String, String> {
        val date = dataPoint.timestamp.toString().split("T")[0]
        val time = dataPoint.timestamp.toString().split("T")[1].split(".")[0]
        val amount = dataPoint.value["amount"]?.toString() ?: "0"
        val coffeeType = dataPoint.value["coffee_type"]?.toString() ?: "coffee"
        val caffeine = dataPoint.value["caffeine_mg"]?.toString() ?: "0"
        val timeOfDay = dataPoint.metadata?.get("time_of_day") ?: ""
        
        return mapOf(
            "Date" to date,
            "Time" to time,
            "Amount (cups)" to amount,
            "Coffee Type" to coffeeType,
            "Caffeine (mg)" to caffeine,
            "Time of Day" to timeOfDay
        )
    }
    
    /**
     * Estimate caffeine content based on coffee type and amount
     * Standard estimates in mg per cup
     */
    private fun estimateCaffeineContent(cups: Double, coffeeType: String): Double {
        val caffeinePerCup = when (coffeeType.lowercase()) {
            "espresso", "espresso shot" -> 63.0  // Single shot
            "americano" -> 154.0
            "cappuccino", "latte", "macchiato" -> 77.0  // Single shot base
            "mocha" -> 95.0
            "cold brew" -> 200.0
            "decaf" -> 3.0
            "instant" -> 62.0
            else -> 95.0  // Regular drip coffee default
        }
        
        return cups * caffeinePerCup
    }
    
    /**
     * Get time of day context for better insights
     */
    private fun getTimeOfDay(): String {
        val hour = java.time.LocalTime.now().hour
        return when (hour) {
            in 5..11 -> "morning"
            in 12..16 -> "afternoon"
            in 17..19 -> "evening"
            else -> "late"
        }
    }
}
