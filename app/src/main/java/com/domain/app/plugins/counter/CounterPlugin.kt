package com.domain.app.plugins.counter

import android.content.Context
import com.domain.app.core.data.DataPoint
import com.domain.app.core.plugin.Plugin
import com.domain.app.core.plugin.PluginMetadata
import com.domain.app.core.plugin.PluginCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

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
    
    private val dataFlow = MutableSharedFlow<DataPoint>()
    private var isActive = false
    private var counter = 0
    
    override suspend fun initialize(context: Context) {
        // No special initialization needed
    }
    
    override suspend fun startCollection() {
        isActive = true
    }
    
    override suspend fun stopCollection() {
        isActive = false
    }
    
    override fun isCollecting(): Boolean = isActive
    
    override fun dataFlow(): Flow<DataPoint> = dataFlow
    
    override fun supportsManualEntry(): Boolean = true
    
    override suspend fun createManualEntry(data: Map<String, Any>): DataPoint? {
        counter++
        val dataPoint = DataPoint(
            pluginId = id,
            type = "count",
            value = mapOf(
                "count" to counter,
                "label" to (data["label"] ?: "Item")
            )
        )
        dataFlow.emit(dataPoint)
        return dataPoint
    }
    
    override suspend fun cleanup() {
        // No cleanup needed
    }
}
