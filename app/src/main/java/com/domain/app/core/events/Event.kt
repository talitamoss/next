package com.domain.app.core.events

import com.domain.app.core.data.DataPoint
import java.time.Instant

/**
 * Base event interface for the event bus system
 */
sealed interface Event {
    val timestamp: Instant
        get() = Instant.now()
    
    /**
     * Plugin-related events
     */
    data class PluginEnabled(
        val pluginId: String,
        override val timestamp: Instant = Instant.now()
    ) : Event
    
    data class PluginDisabled(
        val pluginId: String,
        override val timestamp: Instant = Instant.now()
    ) : Event
    
    data class PluginStartedCollecting(
        val pluginId: String,
        override val timestamp: Instant = Instant.now()
    ) : Event
    
    data class PluginStoppedCollecting(
        val pluginId: String,
        override val timestamp: Instant = Instant.now()
    ) : Event
    
    data class PluginDataCollected(
        val pluginId: String,
        val dataPoint: DataPoint,
        override val timestamp: Instant = Instant.now()
    ) : Event
    
    data class PluginError(
        val pluginId: String,
        val error: Throwable,
        override val timestamp: Instant = Instant.now()
    ) : Event
    
    /**
     * Data-related events
     */
    data class DataPointSaved(
        val dataPoint: DataPoint,
        override val timestamp: Instant = Instant.now()
    ) : Event
    
    data class DataPointDeleted(
        val dataPointId: String,
        override val timestamp: Instant = Instant.now()
    ) : Event
    
    data class DataExported(
        val pluginId: String?,
        val format: String,
        val recordCount: Int,
        override val timestamp: Instant = Instant.now()
    ) : Event
    
    /**
     * System events
     */
    data class BackupCompleted(
        val success: Boolean,
        val recordCount: Int,
        override val timestamp: Instant = Instant.now()
    ) : Event
    
    data class SyncCompleted(
        val success: Boolean,
        val syncedRecords: Int,
        override val timestamp: Instant = Instant.now()
    ) : Event
    
    data class NetworkStatusChanged(
        val isOnline: Boolean,
        override val timestamp: Instant = Instant.now()
    ) : Event
    
    /**
     * Security events
     */
    data class SecurityViolationDetected(
        val pluginId: String,
        val violationType: String,
        override val timestamp: Instant = Instant.now()
    ) : Event
    
    data class PermissionGranted(
        val pluginId: String,
        val permission: String,
        override val timestamp: Instant = Instant.now()
    ) : Event
    
    data class PermissionRevoked(
        val pluginId: String,
        val permission: String,
        override val timestamp: Instant = Instant.now()
    ) : Event
}
