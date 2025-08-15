package com.domain.app.plugins

import android.content.Context
import com.domain.app.core.data.DataPoint
import com.domain.app.core.plugin.*
import com.domain.app.core.plugin.security.*
import com.domain.app.core.validation.ValidationResult
import java.time.Instant

/**
 * Caffeine tracking plugin
 * Simple tracker for coffee, tea, and other caffeinated beverages
 */
class CaffeinePlugin : Plugin {
    override val id = "caffeine"
    
    override val metadata = PluginMetadata(
        name = "Caffeine",
        description = "Track your caffeine intake",
        version = "1.0.0",
        author = "System",
        category = PluginCategory.LIFESTYLE,
        tags = listOf("caffeine", "coffee", "tea", "energy", "drinks", "lifestyle"),
        dataPattern = DataPattern.CUMULATIVE,
        inputType = InputType.CHOICE,  // Will use tile selection UI
        supportsMultiStage = false,
        relatedPlugins = listOf("sleep", "energy", "mood"),
        exportFormat = ExportFormat.CSV,
        dataSensitivity = DataSensitivity.NORMAL,
        naturalLanguageAliases = listOf(
            "coffee", "had coffee", "drank coffee",
            "tea", "energy drink", "double shot",
            "triple shot", "espresso", "caffeine"
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
        dataSensitivity = DataSensitivity.NORMAL,
        dataAccess = setOf(DataAccessScope.OWN_DATA_ONLY),
        privacyPolicy = "Caffeine data is stored locally and never shared without your permission.",
        dataRetention = DataRetentionPolicy.USER_CONTROLLED
    )
    
    override val trustLevel = PluginTrustLevel.OFFICIAL
    
    override fun getPermissionRationale() = mapOf(
        PluginCapability.COLLECT_DATA to "Record your caffeine intake",
        PluginCapability.READ_OWN_DATA to "View your caffeine history",
        PluginCapability.LOCAL_STORAGE to "Save your caffeine data on your device",
        PluginCapability.EXPORT_DATA to "Export your caffeine data for personal use"
    )
    
    override suspend fun initialize(context: Context) {
        // No special initialization needed
    }
    
    override fun supportsManualEntry() = true
    
    override fun supportsAutomaticCollection() = false
    
    override fun getQuickAddConfig() = QuickAddConfig(
        id = "caffeine",
        title = "Log Caffeine",
        inputType = InputType.CHOICE,  // This should trigger tile/choice selection
        options = validTypes.map { type ->
            QuickOption(
                label = type,
                value = type
            )
        },
        primaryColor = "#6F4E37",  // Coffee brown
        secondaryColor = "#D2B48C"   // Light coffee
    )
    
    override suspend fun collectData(): DataPoint? {
        // Automatic collection not supported
        return null
    }
    
    override suspend fun createManualEntry(data: Map<String, Any>): DataPoint? {
        // For carousel input, the value comes as "value" key
        val type = data["value"] as? String ?: data["type"] as? String ?: return null
        val timestamp = (data["timestamp"] as? Instant) ?: Instant.now()
        
        // Validate the entry
        val validationResult = validateDataPoint(data)
        if (validationResult is ValidationResult.Error) {
            return null
        }
        
        return DataPoint(
            id = "caffeine_${System.currentTimeMillis()}",
            pluginId = id,
            timestamp = timestamp,
            type = "caffeine_intake",
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
            type == null -> ValidationResult.Error("Caffeine type is required")
            type !in validTypes -> ValidationResult.Error("Invalid caffeine type")
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
            "Coffee",
            "Double",
            "Triple",
            "BlackTea",
            "GreenTea",
            "Energy",
            "Pills",
            "Other"
        )
    }
}
