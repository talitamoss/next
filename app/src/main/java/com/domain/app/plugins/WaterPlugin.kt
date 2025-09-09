package com.domain.app.plugins

import android.content.Context
import com.domain.app.core.data.DataPoint
import com.domain.app.core.plugin.*
import com.domain.app.core.plugin.security.*
import com.domain.app.core.validation.ValidationResult
import java.time.Instant
import kotlin.math.roundToInt

/**
 * Water intake tracking plugin
 * Simple horizontal slider for 0-2000ml input
 * Uses standard "value" key for consistency across all plugins
 * All values stored and displayed as integers (no decimals)
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
        id = "value",  // STANDARD KEY for all single-input plugins
        title = "Add Water",
        inputType = InputType.HORIZONTAL_SLIDER,
        min = 0f,
        max = 2000f,
        step = 10f,
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
        // Extract value and convert to integer immediately
        val rawValue = (data["value"] as? Number) ?: return null
        val value = rawValue.toFloat().roundToInt()  // Round to nearest integer
        
        // Validate using integer value
        val validationResult = validateDataPoint(mapOf("value" to value))
        if (validationResult is ValidationResult.Error) {
            return null
        }
        
        // Create data point - store as Int
        return DataPoint(
            pluginId = id,
            type = "water_intake",
            value = mapOf(
                "amount" to value,  // Stored as Int
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
        // Convert to integer for validation
        val value = when (val v = data["value"]) {
            is Number -> v.toInt()
            else -> null
        }
        
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
        // Always format as integer
        val amount = when (val amt = dataPoint.value["amount"]) {
            is Number -> amt.toInt().toString()
            else -> "0"
        }
        
        return mapOf(
            "Date" to date,
            "Time" to time,
            "Amount (ml)" to amount
        )
    }
    
    /**
     * Custom formatter to ensure integer display throughout the app
     * Following SleepPlugin pattern with inner class
     */
    override fun getDataFormatter(): PluginDataFormatter = WaterDataFormatter()
    
    /**
     * Inner class for custom water data formatting
     * Ensures all water amounts display as integers without decimals
     */
    private inner class WaterDataFormatter : PluginDataFormatter {
        
        override fun formatSummary(dataPoint: DataPoint): String {
            // Extract amount and ensure integer display
            val amount = when (val amt = dataPoint.value["amount"]) {
                is Number -> amt.toInt()
                else -> 0
            }
            val unit = dataPoint.value["unit"]?.toString() ?: "ml"
            
            // Return clean integer format: "250 ml" not "250.0 ml"
            return "$amount $unit"
        }
        
        override fun formatDetails(dataPoint: DataPoint): List<DataField> {
            val fields = mutableListOf<DataField>()
            
            // Add amount field - formatted as integer
            val amount = when (val amt = dataPoint.value["amount"]) {
                is Number -> amt.toInt()
                else -> 0
            }
            fields.add(
                DataField(
                    label = "Amount",
                    value = "$amount ml",
                    isImportant = true
                )
            )
            
            // Add time of intake (safe null handling)
            val timeParts = dataPoint.timestamp.toString().split("T")
            if (timeParts.size > 1) {
                val timeSubParts = timeParts[1].split(".")
                if (timeSubParts.isNotEmpty() && timeSubParts[0].length >= 5) {
                    fields.add(
                        DataField(
                            label = "Time",
                            value = timeSubParts[0].substring(0, 5)  // HH:mm
                        )
                    )
                }
            }
            
            // Add note if exists
            dataPoint.value["note"]?.let { note ->
                if (note.toString().isNotBlank()) {
                    fields.add(
                        DataField(
                            label = "Note",
                            value = note.toString(),
                            isLongText = true
                        )
                    )
                }
            }
            
            return fields
        }
        
        override fun getHiddenFields(): List<String> = listOf(
            "metadata", 
            "timestamp", 
            "source", 
            "version", 
            "inputType",
            "unit"  // Hide unit since we include it in the formatted value
        )
    }
}
