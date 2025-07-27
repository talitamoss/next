package com.domain.app.core.storage.dao

import androidx.room.*
import com.domain.app.core.storage.entity.DataPointEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface DataPointDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dataPoint: DataPointEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(dataPoints: List<DataPointEntity>)
    
    @Query("SELECT * FROM data_points WHERE pluginId = :pluginId ORDER BY timestamp DESC")
    fun getByPlugin(pluginId: String): Flow<List<DataPointEntity>>
    
    @Query("SELECT * FROM data_points WHERE pluginId = :pluginId AND timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    suspend fun getByPluginAndTimeRange(
        pluginId: String,
        start: Instant,
        end: Instant
    ): List<DataPointEntity>
    
    @Query("SELECT * FROM data_points WHERE timestamp >= :since ORDER BY timestamp DESC")
    fun getRecentData(since: Instant): Flow<List<DataPointEntity>>
    
    @Query("SELECT * FROM data_points WHERE synced = 0")
    suspend fun getUnsyncedData(): List<DataPointEntity>
    
    @Query("UPDATE data_points SET synced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<String>)
    
    @Query("DELETE FROM data_points WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Instant)
    
    @Query("SELECT COUNT(*) FROM data_points WHERE pluginId = :pluginId")
    suspend fun getCountByPlugin(pluginId: String): Int
    
    @Query("SELECT * FROM data_points ORDER BY timestamp DESC LIMIT :limit")
    fun getLatestDataPoints(limit: Int): Flow<List<DataPointEntity>>
}
