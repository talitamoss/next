package com.domain.app.plugins.wellness.water

import android.content.Context
import com.domain.app.core.data.DataPoint
import com.domain.app.core.plugin.Plugin
import com.domain.app.core.plugin.PluginMetadata
import com.domain.app.core.plugin.PluginCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import java.time.Instant

class WaterPlugin : Plugin {
    override val id = "water"
    
    override val metadata = PluginMetadata(
        name = "Water Intake",
        description = "Track your daily water consumption",
        version = "1.0.0",
        author = "System",
        category = PluginCategory.HEALTH
    )
    
    private val dataFlow = MutableSharedFlow<DataPoint>()
    private var isActive = false
    
    override suspend fun initialize(context: Context) {
        // No special initialization needed for water tracking
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
        val amount = data["amount"] as? Number ?: return null
        val unit = data["unit"] as? String ?: "ml"
        
        val dataPoint = DataPoint(
            pluginId = id,
            timestamp = Instant.now(),
            type = "water_intake",
            value = mapOf(
                "amount" to amount.toDouble(),
                "unit" to unit
            ),
            metadata = mapOf(
                "source" to "manual_entry"
            )
        )
        
        dataFlow.emit(dataPoint)
        return dataPoint
    }
    
    override suspend fun cleanup() {
        // No cleanup needed
    }
}
