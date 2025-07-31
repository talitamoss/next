package com.domain.app.core.events

import com.domain.app.core.events.Event
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central event bus for application-wide event distribution
 */
@Singleton
class EventBus @Inject constructor() {
    private val _events = MutableSharedFlow<Event>(
        replay = 0,
        extraBufferCapacity = 64
    )
    
    val events: SharedFlow<Event> = _events.asSharedFlow()
    
    /**
     * Post an event to all subscribers
     */
    suspend fun post(event: Event) {
        _events.emit(event)
    }
    
    /**
     * Try to post an event without suspending
     */
    fun tryPost(event: Event): Boolean {
        return _events.tryEmit(event)
    }
    
    companion object {
        // Global instance for static access (use with caution)
        private var instance: EventBus? = null
        
        fun setInstance(eventBus: EventBus) {
            instance = eventBus
        }
        
        fun post(event: Event) {
            instance?.tryPost(event)
        }
    }
}
