package com.domain.app.core.event

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Central event bus for application-wide communication
 */
object EventBus {
    private val _events = MutableSharedFlow<Event>()
    val events: SharedFlow<Event> = _events.asSharedFlow()
    
    suspend fun emit(event: Event) {
        _events.emit(event)
    }
}

sealed class Event {
    data class DataCollected(val dataPoint: com.domain.app.core.data.DataPoint) : Event()
    data class PluginStateChanged(val pluginId: String, val isActive: Boolean) : Event()
    data class SyncRequested(val pluginId: String?) : Event()
    data class PermissionRequired(val permission: String) : Event()
}
