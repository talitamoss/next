package com.domain.app.core.data

import java.time.Instant
import java.util.UUID

/**
 * Simplified DataPoint for MVP with slots for future expansion
 */
data class DataPoint(
    // Essential fields only
    val id: String = UUID.randomUUID().toString(),
    val pluginId: String,
    val timestamp: Instant = Instant.now(),
    val type: String,
    
    // Simple but flexible data storage
    val value: Map<String, Any>,
    
    // Future-proofing fields (nullable for now)
    val metadata: Map<String, String>? = null,
    val source: String? = "manual",  // "manual", "voice", "auto"
    val version: Int = 1
)

/**
 * Location data if needed
 */
data class Location(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float? = null,
    val altitude: Double? = null
)
