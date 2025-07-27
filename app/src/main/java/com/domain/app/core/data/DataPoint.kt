package com.domain.app.core.data

import java.time.Instant
import java.util.UUID

/**
 * Universal data point that all plugins produce
 */
data class DataPoint(
    val id: String = UUID.randomUUID().toString(),
    val pluginId: String,
    val timestamp: Instant = Instant.now(),
    val type: String,
    val value: Map<String, Any>,
    val metadata: Map<String, String> = emptyMap(),
    val location: Location? = null
)

data class Location(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float? = null,
    val altitude: Double? = null
)
