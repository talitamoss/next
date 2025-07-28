package com.domain.app.plugins.counter

import android.content.Context
import com.domain.app.core.data.DataPoint
import com.domain.app.core.plugin.*

/**
 * Simple counter plugin for testing the plugin system
 */
class CounterPlugin : Plugin {
    override val id = "counter"
    
    override val metadata = PluginMetadata(
        name = "Counter",
        description = "Simple counting plugin",
        version = "1.0.0",
        author = "System",
        category = PluginCategory.OTHER
    )
    
    private var counter = 0
    
    override suspend fun initialize(context: Context) {
        // No special initialization needed
    }
    
    override fun supportsManualEntry(): Boolean = true
    
    override fun getQuickAddConfig(): QuickAddConfig {
        return QuickAddConfig(
            title = "Increment Counter",
            inputType = InputType.CHOICE,
            options = listOf(
                QuickOption("+1", 1),
                QuickOption("+5", 5),
                QuickOption("+10", 10)
            )
        )
    }
    
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
    
    override suspend fun cleanup() {
        // No cleanup needed
    }
}
