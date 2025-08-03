package com.domain.app.core

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Simple event bus for app-wide event communication
 * Uses Kotlin Flow for reactive event streams
 * 
 * File location: app/src/main/java/com/domain/app/core/EventBus.kt
 */
object EventBus {
    private val _events = MutableSharedFlow<Any>()
    val events: SharedFlow<Any> = _events.asSharedFlow()
    
    suspend fun post(event: Any) {
        _events.emit(event)
    }
    
    inline fun <reified T> subscribe(): SharedFlow<T> {
        return _events
            .asSharedFlow()
            .filter { it is T }
            .map { it as T }
    }
}

/**
 * Extension function to filter events by type
 */
inline fun <reified T> SharedFlow<Any>.filterIsInstance(): SharedFlow<T> {
    return this
        .filter { it is T }
        .map { it as T }
}

/**
 * Extension functions for Flow
 */
fun <T> SharedFlow<T>.filter(predicate: (T) -> Boolean): SharedFlow<T> {
    val flow = MutableSharedFlow<T>()
    this.onEach { value ->
        if (predicate(value)) {
            flow.emit(value)
        }
    }
    return flow.asSharedFlow()
}

fun <T, R> SharedFlow<T>.map(transform: (T) -> R): SharedFlow<R> {
    val flow = MutableSharedFlow<R>()
    this.onEach { value ->
        flow.emit(transform(value))
    }
    return flow.asSharedFlow()
}

fun <T> SharedFlow<T>.onEach(action: suspend (T) -> Unit): SharedFlow<T> {
    kotlinx.coroutines.GlobalScope.launch {
        this@onEach.collect { value ->
            action(value)
        }
    }
    return this
}
