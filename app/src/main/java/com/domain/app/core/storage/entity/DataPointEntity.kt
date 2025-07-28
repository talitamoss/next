package com.domain.app.core.storage.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Database entity for data points
 * Designed to be flexible for future expansion without migrations
 */
@Entity(tableName = "data_points")
data class DataPointEntity(
    @PrimaryKey
    val id: String,
    val pluginId: String,
    val timestamp: Instant,
    val type: String,
    
    // Store complex data as JSON
    val valueJson: String,
    
    // Future fields (nullable)
    val metadataJson: String? = null,
    val source: String? = null,
    val version: Int = 1,
    
    // Sync support
    val synced: Boolean = false,
    val createdAt: Instant = Instant.now(),
    
    // Reserved columns for future use without migration
    val extra1: String? = null,  // Could be used for embeddings
    val extra2: String? = null,  // Could be used for nlp context
    val extra3: Long? = null,    // Could be used for flags
    val extra4: Double? = null   // Could be used for confidence scores
)
