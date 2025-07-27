package com.domain.app.core.storage.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "plugin_states")
data class PluginStateEntity(
    @PrimaryKey
    val pluginId: String,
    val isEnabled: Boolean = true,
    val isCollecting: Boolean = false,
    val lastCollection: Instant? = null,
    val configuration: String = "{}", // JSON string for plugin-specific config
    val errorCount: Int = 0,
    val lastError: String? = null
)
