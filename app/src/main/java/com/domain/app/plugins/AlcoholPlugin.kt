package com.domain.app.plugins

import android.content.Context
import com.domain.app.core.data.DataPoint
import com.domain.app.core.plugin.*
import com.domain.app.core.plugin.security.*
import com.domain.app.core.validation.ValidationResult
import java.time.Instant

/**
 * Alcohol consumption tracking plugin
 * Simple tracker for beer, wine, spirits, and other alcoholic beverages
 */
class AlcoholPlugin : Plugin {
    override val id = "alcohol"
    
    override val metadata = PluginMetadata(
        name = "Alcohol",
        description = "Track your alcohol consumption",
        version = "1.0.0",
        author = "System",
        category = PluginCategory.LIFESTYLE,
        tags = listOf("alcohol", "drinks", "beer", "wine", "spirits", "lifestyle", "health"),
        dataPattern = DataPattern.CUMULATIVE,
        inputType = InputType.CHOICE,  // Will use tile selection UI
        supportsMultiStage = false,
        relatedPlugins = listOf("sleep", "mood", "water", "caffeine"),
        exportFormat = ExportFormat.CSV,
        dataSensitivity = DataSensitivity.SENSITIVE,  // More sensitive than caffeine
        naturalLanguageAliases = listOf(
            "alcohol", "had a drink", "drank alcohol",
            "beer", "wine", "whiskey", "vodka", "rum",
            "cocktail", "shot", "pint", "glass of wine",
            "drinks", "drinking", "nightcap"
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
            PluginCapability.EXPORT_DATA
        ),
        dataSensitivity = DataSensitivity.SENSITIVE,
        dataAccess = setOf(DataAccessScope.OWN_DATA_ONLY),
        privacyPolicy = "Alcohol consumption data is stored locally and encrypted. This sensitive health data is never shared without your explicit permission.",
        dataRetention = DataRetentionPolicy.USER_CONTROLLED
    )
    
    override val trustLevel = PluginTrustLevel.OFFICIAL
    
    override fun getPermissionRationale() = mapOf(
        PluginCapability.COLLECT_DATA to "Record your alcohol consumption",
        PluginCapability.READ_OWN_DATA to "View your drinking history",
        PluginCapability.LOCAL_STORAGE to "Save your alcohol data securely on your device",
        PluginCapability.EXPORT_DATA to "Export your alcohol data for personal use or medical purposes"
    )
    
    override suspend fun initialize(context: Context) {
        // No special initialization needed
    }
    
    override fun supportsManualEntry() = true
    
    override fun supportsAutomaticCollection() = false
    
    override fun getQuickAddConfig() = QuickAddConfig(
        id = "alcohol",
        title = "Log Alcohol",
        inputType = InputType.CHOICE,  // This should trigger tile/choice selection
        options = validTypes.map { type ->
            QuickOption(
                label = type,
                value = type
            )
        },
        primaryColor = "#8B4513",  // Saddle brown (beer/whiskey color)
        secondaryColor = "#F4A460"   // Sandy brown (light beer color)
    )
    
    override suspend fun collectData(): DataPoint? {
        // Automatic collection not supported
        return null
    }
    
    override suspend fun createManualEntry(data: Map<String, Any>): DataPoint? {
        // For choice input, the value comes as "value" key
        val type = data["value"] as? String ?: data["type"] as? String ?: return null
        val timestamp = (data["timestamp"] as? Instant) ?: Instant.now()
        
        // Validate the entry
        val validationResult = validateDataPoint(data)
        if (validationResult is ValidationResult.Error) {
            return null
        }
        
        return DataPoint(
            id = "alcohol_${System.currentTimeMillis()}",
            pluginId = id,
            timestamp = timestamp,
            type = "alcohol_intake",
            value = mapOf(
                "type" to type
            ),
            metadata = mapOf(
                "version" to metadata.version,
                "inputType" to "manual"
            )
        )
    }
    
    override fun validateDataPoint(data: Map<String, Any>): ValidationResult {
        val type = data["value"] as? String ?: data["type"] as? String
        
        return when {
            type == null -> ValidationResult.Error("Alcohol type is required")
            type !in validTypes -> ValidationResult.Error("Invalid alcohol type")
            else -> ValidationResult.Success
        }
    }
    
    override fun exportHeaders(): List<String> {
        return listOf("Date", "Time", "Type")
    }
    
    override fun formatForExport(dataPoint: DataPoint): Map<String, String> {
        val date = dataPoint.timestamp.toString().split("T")[0]
        val time = dataPoint.timestamp.toString().split("T")[1].split(".")[0]
        val type = dataPoint.value["type"]?.toString() ?: "Unknown"
        
        return mapOf(
            "Date" to date,
            "Time" to time,
            "Type" to type
        )
    }
    
    companion object {
        val validTypes = listOf(
            "Beer",       // Standard beer/pint
            "Wine",       // Glass of wine
            "Spirits",    // Shot of spirits
            "Cocktail",   // Mixed drink
            "Cider",      // Hard cider
            "Moonshine"   // Moonshine/homemade spirits
        )
    }
}
