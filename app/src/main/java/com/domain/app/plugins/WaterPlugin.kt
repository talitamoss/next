package com.domain.app.plugins

import android.content.Context
import com.domain.app.core.data.DataPoint
import com.domain.app.core.plugin.*
import com.domain.app.core.plugin.security.*

/**
 * Water intake tracking plugin with security manifest
 */
class WaterPlugin : Plugin {
    override val id = "water"
    
    override val supportsAutomaticCollection = false
    
    override val metadata = PluginMetadata(
        name = "Water Intake",
        description = "Track your daily water consumption",
        version = "1.0.0",
        author = "System",
        category = PluginCategory.HEALTH,
        tags = listOf("hydration", "health", "daily", "quantitative"),
        dataPattern = DataPattern.CUMULATIVE,
        inputType = InputType.CHOICE,
        supportsMultiStage = false,
        relatedPlugins = listOf("exercise", "sleep"),
        exportFormat = ExportFormat.CSV,
        dataSensitivity = DataSensitivity.NORMAL,
        naturalLanguageAliases = listOf(
            "water", "hydration", "drink", "fluid", "h2o",
            "drank", "drinking", "thirsty", "glass of water",
            "bottle of water", "hydrate"
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
        privacyPolicy = "Water intake data is stored locally and never shared without your permission.",
        dataRetention = DataRetentionPolicy.USER_CONTROLLED
    )
    
    override val trustLevel = PluginTrustLevel.OFFICIAL
    
    override fun getPermissionRationale() = mapOf(
        PluginCapability.COLLECT_DATA to "Record your daily water intake",
        PluginCapability.READ_OWN_DATA to "View your hydration history and progress",
        PluginCapability.LOCAL_STORAGE to "Save your water intake data on your device",
        PluginCapability.EXPORT_DATA to "Export your hydration data for personal use"
    )
    
    override suspend fun initialize(context: Context) {
        // No special initialization needed
    }
    
    override fun supportsManualEntry() = true
    
    override fun getQuickAddConfig() = QuickAddConfig(
        title = "Add Water",
        defaultValue = 250,
        inputType = InputType.CHOICE,
        options = listOf(
            QuickOption("Glass", 250, "💧"),
            QuickOption("Bottle", 500, "💧"),
            QuickOption("Liter", 1000, "💧"),
            QuickOption("Custom", -1, "💧")
        ),
        unit = "ml"
    )
    
    override suspend fun createManualEntry(data: Map<String, Any>): DataPoint? {
        val amount = when (val value = data["value"] ?: data["amount"]) {
            is Number -> value.toInt()
            else -> 250 // Default to glass
        }
        
        return DataPoint(
            pluginId = id,
            type = "water_intake",
            value = mapOf(
                "amount" to amount,
                "unit" to "ml"
            ),
            source = "manual"
        )
    }
    
    override fun validateDataPoint(data: Map<String, Any>): ValidationResult {
        val amount = (data["value"] as? Number)?.toInt() 
            ?: (data["amount"] as? Number)?.toInt()
        
        return when {
            amount == null -> ValidationResult.Success // Default to 250ml
            amount <= 0 -> ValidationResult.Error("Amount must be positive")
            amount > 5000 -> ValidationResult.Warning("That's a lot of water at once! Are you sure?")
            else -> ValidationResult.Success
        }
    }
    
    override fun exportHeaders() = listOf(
        "Date", "Time", "Amount (ml)"
    )
    
    override fun formatForExport(dataPoint: DataPoint): Map<String, String> {
        val date = dataPoint.timestamp.toString().split("T")[0]
        val time = dataPoint.timestamp.toString().split("T")[1].split(".")[0]
        
        return mapOf(
            "Date" to date,
            "Time" to time,
            "Amount (ml)" to (dataPoint.value["amount"]?.toString() ?: "")
        )
    }
}
