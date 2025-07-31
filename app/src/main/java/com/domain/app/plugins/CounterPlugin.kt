package com.domain.app.plugins

import android.content.Context
import com.domain.app.core.data.DataPoint
import com.domain.app.core.plugin.*
import com.domain.app.core.plugin.security.*

/**
 * Simple counter plugin with minimal security requirements
 */
class CounterPlugin : Plugin {
    override val id = "counter"
    
    override val metadata = PluginMetadata(
override val supportsAutomaticCollection = false
        name = "Counter",
        description = "Simple counting plugin",
        version = "1.0.0",
        author = "System",
        category = PluginCategory.OTHER,
        tags = listOf("counter", "tally", "count", "simple"),
        dataPattern = DataPattern.CUMULATIVE,
        inputType = InputType.CHOICE,
        supportsMultiStage = false,
        relatedPlugins = emptyList(),
        exportFormat = ExportFormat.CSV,
        dataSensitivity = DataSensitivity.NORMAL,
        naturalLanguageAliases = listOf(
            "count", "counter", "tally", "increment",
            "add one", "plus one", "tick", "mark"
        )
    )
    
    override val securityManifest = PluginSecurityManifest(
        requestedCapabilities = setOf(
            PluginCapability.COLLECT_DATA,
            PluginCapability.READ_OWN_DATA,
            PluginCapability.LOCAL_STORAGE
        ),
        dataSensitivity = DataSensitivity.NORMAL,
        dataAccess = setOf(DataAccessScope.OWN_DATA_ONLY),
        privacyPolicy = "Counter data is stored locally for tracking purposes only.",
        dataRetention = DataRetentionPolicy.DEFAULT
    )
    
    override val trustLevel = PluginTrustLevel.OFFICIAL
    
    override fun getPermissionRationale() = mapOf(
        PluginCapability.COLLECT_DATA to "Count and track items or events",
        PluginCapability.READ_OWN_DATA to "View your counting history",
        PluginCapability.LOCAL_STORAGE to "Save your counts on your device"
    )
    
    private var counter = 0
    
    override suspend fun initialize(context: Context) {
        // Could load last counter value from preferences
    }
    
    override fun supportsManualEntry() = true
    
    override fun getQuickAddConfig() = QuickAddConfig(
        title = "Increment Counter",
        inputType = InputType.CHOICE,
        options = listOf(
            QuickOption("+1", 1),
            QuickOption("+5", 5),
            QuickOption("+10", 10),
            QuickOption("Custom", -1)
        )
    )
    
    override suspend fun createManualEntry(data: Map<String, Any>): DataPoint? {
        val increment = when (val value = data["value"] ?: data["amount"]) {
            is Number -> value.toInt()
            else -> 1
        }
        
        counter += increment
        
        val label = data["label"] as? String ?: "Item"
        
        return DataPoint(
            pluginId = id,
            type = "count",
            value = mapOf(
                "count" to counter,
                "increment" to increment,
                "label" to label
            ),
            source = "manual"
        )
    }
    
    override fun validateDataPoint(data: Map<String, Any>): ValidationResult {
        val increment = (data["value"] as? Number)?.toInt() 
            ?: (data["amount"] as? Number)?.toInt()
        
        return when {
            increment == null -> ValidationResult.Success // Default to 1
            increment < 0 -> ValidationResult.Error("Increment must be positive")
            increment > 1000 -> ValidationResult.Warning("That's a large increment! Are you sure?")
            else -> ValidationResult.Success
        }
    }
    
    override fun exportHeaders() = listOf(
        "Date", "Time", "Total Count", "Increment", "Label"
    )
    
    override fun formatForExport(dataPoint: DataPoint): Map<String, String> {
        val date = dataPoint.timestamp.toString().split("T")[0]
        val time = dataPoint.timestamp.toString().split("T")[1].split(".")[0]
        
        return mapOf(
            "Date" to date,
            "Time" to time,
            "Total Count" to (dataPoint.value["count"]?.toString() ?: ""),
            "Increment" to (dataPoint.value["increment"]?.toString() ?: ""),
            "Label" to (dataPoint.value["label"]?.toString() ?: "")
        )
    }
}
