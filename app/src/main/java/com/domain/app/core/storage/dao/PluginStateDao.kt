package com.domain.app.core.storage.dao

import androidx.room.*
import com.domain.app.core.storage.entity.PluginStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PluginStateDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(state: PluginStateEntity)
    
    @Query("SELECT * FROM plugin_states WHERE pluginId = :pluginId")
    suspend fun getState(pluginId: String): PluginStateEntity?
    
    @Query("SELECT * FROM plugin_states")
    fun getAllStates(): Flow<List<PluginStateEntity>>
    
    @Query("SELECT * FROM plugin_states WHERE isEnabled = 1")
    suspend fun getEnabledPlugins(): List<PluginStateEntity>
    
    @Query("UPDATE plugin_states SET isCollecting = :collecting WHERE pluginId = :pluginId")
    suspend fun updateCollectingState(pluginId: String, collecting: Boolean)
    
    @Query("UPDATE plugin_states SET configuration = :config WHERE pluginId = :pluginId")
    suspend fun updateConfiguration(pluginId: String, config: String)
    
    @Query("UPDATE plugin_states SET errorCount = errorCount + 1, lastError = :error WHERE pluginId = :pluginId")
    suspend fun recordError(pluginId: String, error: String)
    
    @Query("UPDATE plugin_states SET errorCount = 0, lastError = NULL WHERE pluginId = :pluginId")
    suspend fun clearErrors(pluginId: String)
}
