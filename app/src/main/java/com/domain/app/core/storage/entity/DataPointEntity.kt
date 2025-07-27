package com.domain.app.core.storage.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "data_points")
data class DataPointEntity(
    @PrimaryKey
    val id: String,
    val pluginId: String,
    val timestamp: Instant,
    val type: String,
    val value: String, // JSON string
    val metadata: String, // JSON string
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationAccuracy: Float? = null,
    val altitude: Double? = null,
    val synced: Boolean = false,
    val createdAt: Instant = Instant.now()
)
