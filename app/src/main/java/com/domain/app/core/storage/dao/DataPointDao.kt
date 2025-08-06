// app/src/main/java/com/domain/app/core/storage/dao/DataPointDao.kt
package com.domain.app.core.storage.dao

import androidx.room.*
import com.domain.app.core.storage.entity.DataPointEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Data Access Object for DataPoint entities
 */
@Dao
interface DataPointDao {
    
    // Insert operations
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dataPoint: DataPointEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(dataPoints: List<DataPointEntity>)
    
    @Upsert
    suspend fun upsert(dataPoint: DataPointEntity)
    
    // Query operations
    
    @Query("SELECT * FROM data_points WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): DataPointEntity?
    
    @Query("SELECT * FROM data_points ORDER BY timestamp DESC LIMIT :limit")
    fun getLatestDataPoints(limit: Int): Flow<List<DataPointEntity>>
    
    @Query("SELECT * FROM data_points WHERE plugin_id = :pluginId ORDER BY timestamp DESC")
    fun getPluginData(pluginId: String): Flow<List<DataPointEntity>>
    
    @Query("SELECT * FROM data_points WHERE plugin_id = :pluginId ORDER BY timestamp DESC")
    suspend fun getAllPluginData(pluginId: String): List<DataPointEntity>
    
    @Query("""
        SELECT * FROM data_points 
        WHERE plugin_id = :pluginId 
        AND timestamp BETWEEN :startTime AND :endTime
        ORDER BY timestamp DESC
    """)
    fun getPluginDataInRange(
        pluginId: String,
        startTime: Instant,
        endTime: Instant
    ): Flow<List<DataPointEntity>>
    
    @Query("""
        SELECT * FROM data_points 
        WHERE timestamp BETWEEN :startTime AND :endTime
        ORDER BY timestamp DESC
    """)
    fun getDataInRange(
        startTime: Instant,
        endTime: Instant
    ): Flow<List<DataPointEntity>>
    
    @Query("SELECT * FROM data_points WHERE value LIKE :query ORDER BY timestamp DESC")
    fun searchDataPoints(query: String): Flow<List<DataPointEntity>>
    
    @Query("SELECT * FROM data_points ORDER BY timestamp DESC")
    suspend fun getAllDataPoints(): List<DataPointEntity>
    
    @Query("SELECT COUNT(*) FROM data_points")
    suspend fun getTotalCount(): Int
    
    @Query("SELECT COUNT(*) FROM data_points WHERE plugin_id = :pluginId")
    suspend fun getPluginDataCount(pluginId: String): Int
    
    @Query("SELECT * FROM data_points WHERE plugin_id = :pluginId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestPluginEntry(pluginId: String): DataPointEntity?
    
    @Query("SELECT * FROM data_points WHERE plugin_id = :pluginId ORDER BY timestamp ASC LIMIT 1")
    suspend fun getOldestPluginEntry(pluginId: String): DataPointEntity?
    
    @Query("""
        SELECT * FROM data_points 
        WHERE timestamp >= :since 
        ORDER BY timestamp DESC
    """)
    fun getDataSince(since: Instant): Flow<List<DataPointEntity>>
    
    @Query("""
        SELECT DISTINCT plugin_id FROM data_points
    """)
    suspend fun getDistinctPluginIds(): List<String>
    
    @Query("""
        SELECT * FROM data_points 
        WHERE sync_status = :syncStatus
        ORDER BY timestamp DESC
    """)
    suspend fun getDataBySyncStatus(syncStatus: String): List<DataPointEntity>
    
    // Update operations
    
    @Update
    suspend fun update(dataPoint: DataPointEntity)
    
    @Update
    suspend fun updateAll(dataPoints: List<DataPointEntity>)
    
    @Query("UPDATE data_points SET sync_status = :syncStatus WHERE id = :id")
    suspend fun updateSyncStatus(id: String, syncStatus: String)
    
    @Query("UPDATE data_points SET sync_status = :syncStatus WHERE id IN (:ids)")
    suspend fun updateSyncStatusBatch(ids: List<String>, syncStatus: String)
    
    // Delete operations
    
    @Delete
    suspend fun delete(dataPoint: DataPointEntity)
    
    @Delete
    suspend fun deleteAll(dataPoints: List<DataPointEntity>)
    
    @Query("DELETE FROM data_points WHERE id = :id")
    suspend fun deleteById(id: String)
    
    @Query("DELETE FROM data_points WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)
    
    @Query("DELETE FROM data_points WHERE plugin_id = :pluginId")
    suspend fun deleteByPluginId(pluginId: String)
    
    @Query("DELETE FROM data_points WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Instant)
    
    @Query("DELETE FROM data_points")
    suspend fun deleteAll()
    
    // Transaction operations
    
    @Transaction
    suspend fun replacePluginData(pluginId: String, newData: List<DataPointEntity>) {
        deleteByPluginId(pluginId)
        insertAll(newData)
    }
    
    @Transaction
    suspend fun deleteAndInsert(toDelete: List<String>, toInsert: List<DataPointEntity>) {
        deleteByIds(toDelete)
        insertAll(toInsert)
    }
}
