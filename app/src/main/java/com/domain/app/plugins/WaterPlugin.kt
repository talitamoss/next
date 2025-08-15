package com.domain.app.plugins

import android.content.Context
import com.domain.app.core.data.DataPoint
import com.domain.app.core.plugin.*
import com.domain.app.core.plugin.security.*
import com.domain.app.core.validation.ValidationResult
import java.time.Instant

/**
 * Water intake tracking plugin
 * Simple horizontal slider for 0-2000ml input
 */
class WaterPlugin : Plugin {
    override val id = "water"
    
    override val metadata = PluginMetadata(
        name = "Water",
        description = "Track your daily water intake",
        version = "1.0.0",
        author = "System",
        category = PluginCategory.HEALTH,
        tags = listOf("water", "hydration", "health", "daily"),
        dataPattern = DataPattern.CUMULATIVE,
        inputType = InputType.HORIZONTAL_SLIDER,
        supportsMultiStage = false,
        relatedPlugins = emptyList(),
        exportFormat = ExportFormat.CSV,
        dataSensitivity = DataSensitivity.NORMAL,
        naturalLanguageAliases = listOf(
            "water", "hydration", "drink", "drank water",
            "glass of water", "bottle of water"
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
        PluginCapability.COLLECT_DATA to "Record your water intake",
        PluginCapability.READ_OWN_DATA to "View your hydration history",
        PluginCapability.LOCAL_STORAGE to "Save your water intake data on your device",
        PluginCapability.EXPORT_DATA to "Export your hydration data for personal use"
    )
    
    override suspend fun initialize(context: Context) {
        // No special initialization needed
    }
    
    override fun supportsManualEntry() = true
    
    override fun getQuickAddConfig() = QuickAddConfig(
        id = "water",
        title = "Add Water",
        inputType = InputType.HORIZONTAL_SLIDER,
        min = 0f,
        max = 2000f,
        step = 50f,
        defaultValue = 250f,
        unit = "ml",
        showValue = true,
        primaryColor = "#2196F3",   // Blue for water
        secondaryColor = "#E3F2FD"   // Light blue for track
    )
    
    override suspend fun collectData(): DataPoint? {
        // Automatic collection not supported
        return null
    }
    
    override suspend fun createManualEntry(data: Map<String, Any>): DataPoint? {
        // Get value from slider
        val amount = when (val value = data["value"]) {
            is Number -> value.toFloat()
            is String -> value.toFloatOrNull() ?: return null
            else -> return null
        }
        
        // Validate
        val validationResult = validateDataPoint(mapOf("value" to amount))
        if (validationResult is ValidationResult.Error) {
            return null
        }
        
        // Create data point
        return DataPoint(
            pluginId = id,
            type = "water_intake",
            value = mapOf(
                "amount" to amount,
                "unit" to "ml"
            ),
            metadata = mapOf(
                "input_method" to "slider"
            ),
            timestamp = Instant.now(),
            source = "manual"
        )
    }
    
    override fun validateDataPoint(data: Map<String, Any>): ValidationResult {
        val value = (data["value"] as? Number)?.toFloat()
        
        return when {
            value == null -> ValidationResult.Error("Water amount is required")
            value < 0 -> ValidationResult.Error("Amount cannot be negative")
            value > 2000 -> ValidationResult.Error("Maximum single entry is 2000ml")
            else -> ValidationResult.Success
        }
    }
    
    override fun exportHeaders() = listOf(
        "Date", "Time", "Amount (ml)"
    )
    
    override fun formatForExport(dataPoint: DataPoint): Map<String, String> {
        val date = dataPoint.timestamp.toString().split("T")[0]
        val time = dataPoint.timestamp.toString().split("T")[1].split(".")[0]
        val amount = dataPoint.value["amount"]?.toString() ?: "0"
        
        return mapOf(
            "Date" to date,
            "Time" to time,
            "Amount (ml)" to amount
        )
    }
}
