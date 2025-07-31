package com.domain.app.core.storage.dao

import androidx.room.*
import com.domain.app.core.storage.entity.PluginStateEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Data Access Object for plugin state management
 */
@Dao
interface PluginStateDao {
    
    @Query("SELECT * FROM plugin_states")
    fun getAllStates(): Flow<List<PluginStateEntity>>
    
    @Query("SELECT * FROM plugin_states WHERE pluginId = :pluginId")
    suspend fun getState(pluginId: String): PluginStateEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(state: PluginStateEntity)
    
    @Query("UPDATE plugin_states SET isEnabled = :enabled WHERE pluginId = :pluginId")
    suspend fun updateEnabledState(pluginId: String, enabled: Boolean)
    
    @Query("UPDATE plugin_states SET isCollecting = :collecting WHERE pluginId = :pluginId")
    suspend fun updateCollectingState(pluginId: String, collecting: Boolean)
    
    @Query("UPDATE plugin_states SET lastCollection = :time WHERE pluginId = :pluginId")
    suspend fun updateCollectionTime(pluginId: String, time: Instant)
    
    @Query("UPDATE plugin_states SET configuration = :config WHERE pluginId = :pluginId")
    suspend fun updateConfiguration(pluginId: String, config: String?)
    
    @Query("UPDATE plugin_states SET errorCount = errorCount + 1, lastError = :error WHERE pluginId = :pluginId")
    suspend fun recordError(pluginId: String, error: String)
    
    @Query("DELETE FROM plugin_states WHERE pluginId = :pluginId")
    suspend fun delete(pluginId: String)
    
    @Query("DELETE FROM plugin_states")
    suspend fun deleteAll()
}

    suspend fun updateLastCollectionTime(pluginId: String, timestamp: Long) {
        // TODO: Implement
    }
    
    suspend fun incrementErrorCount(pluginId: String) {
        // TODO: Implement  
    }

