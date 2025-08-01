package com.domain.app.core.data

import com.domain.app.core.storage.AppDatabase
import com.domain.app.core.storage.entity.DataPointEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataRepository @Inject constructor(
    private val database: AppDatabase
) {
    
    /**
     * Save a data point
     */
    suspend fun saveDataPoint(dataPoint: DataPoint) = withContext(Dispatchers.IO) {
        database.dataPointDao().insert(dataPoint.toEntity())
    }
    
    /**
     * Save multiple data points
     */
    suspend fun saveDataPoints(dataPoints: List<DataPoint>) = withContext(Dispatchers.IO) {
        database.dataPointDao().insertAll(dataPoints.map { it.toEntity() })
    }
    
    /**
     * Get data for a specific plugin
     */
    fun getPluginData(pluginId: String): Flow<List<DataPoint>> {
        return database.dataPointDao().getByPlugin(pluginId)
            .map { entities -> entities.map { it.toDataPoint() } }
    }
    
    /**
     * Get data for a plugin within a time range
     */
    suspend fun getPluginDataInRange(
        pluginId: String,
        start: Instant,
        end: Instant
    ): List<DataPoint> = withContext(Dispatchers.IO) {
        database.dataPointDao()
            .getByPluginAndTimeRange(pluginId, start, end)
            .map { it.toDataPoint() }
    }
    
    /**
     * Get recent data across all plugins
     */
    fun getRecentData(hours: Int = 24): Flow<List<DataPoint>> {
        val since = Instant.now().minus(hours.toLong(), ChronoUnit.HOURS)
        return database.dataPointDao().getRecentData(since)
            .map { entities -> entities.map { it.toDataPoint() } }
    }
    
    /**
     * Get latest data points with limit
     */
    fun getLatestDataPoints(limit: Int): Flow<List<DataPoint>> {
        return database.dataPointDao().getLatestDataPoints(limit)
            .map { entities -> entities.map { it.toDataPoint() } }
    }
    
    /**
     * Get count of data points for a plugin
     */
    suspend fun getDataCount(pluginId: String): Int = withContext(Dispatchers.IO) {
        database.dataPointDao().getCountByPlugin(pluginId)
    }
    
    /**
     * Get unsynced data for backup/sync
     */
    suspend fun getUnsyncedData(): List<DataPoint> = withContext(Dispatchers.IO) {
        database.dataPointDao().getUnsyncedData()
            .map { it.toDataPoint() }
    }
    
    /**
     * Mark data as synced
     */
    suspend fun markDataAsSynced(dataPointIds: List<String>) = withContext(Dispatchers.IO) {
        database.dataPointDao().markAsSynced(dataPointIds)
    }
    
    /**
     * Clean up old data
     */
    suspend fun cleanupOldData(daysToKeep: Int) = withContext(Dispatchers.IO) {
        val cutoffDate = Instant.now().minus(daysToKeep.toLong(), ChronoUnit.DAYS)
        database.dataPointDao().deleteOlderThan(cutoffDate)
    }
    
    /**
     * Get aggregated statistics for a plugin
     */
    suspend fun getPluginStatistics(
        pluginId: String,
        start: Instant,
        end: Instant
    ): PluginStatistics = withContext(Dispatchers.IO) {
        val dataPoints = getPluginDataInRange(pluginId, start, end)
        
        PluginStatistics(
            pluginId = pluginId,
            totalCount = dataPoints.size,
            startDate = start,
            endDate = end,
            latestDataPoint = dataPoints.maxByOrNull { it.timestamp }
        )
    }
}

/**
 * Extension functions for data conversion
 */
fun DataPoint.toEntity(): DataPointEntity {
    return DataPointEntity(
        id = id,
        pluginId = pluginId,
        timestamp = timestamp,
        type = type,
        valueJson = JSONObject(value).toString(),
        metadataJson = metadata?.let { JSONObject(it).toString() },
        source = source,
        version = version
    )
}

fun DataPointEntity.toDataPoint(): DataPoint {
    return DataPoint(
        id = id,
        pluginId = pluginId,
        timestamp = timestamp,
        type = type,
        value = JSONObject(valueJson).toMap(),
        metadata = metadataJson?.let { JSONObject(it).toStringMap() },
        source = source,
        version = version
    )
}

fun JSONObject.toMap(): Map<String, Any> {
    val map = mutableMapOf<String, Any>()
    keys().forEach { key ->
        val value = get(key)
        map[key] = when (value) {
            is JSONObject -> value.toMap()
            else -> value
        }
    }
    return map
}

fun JSONObject.toStringMap(): Map<String, String> {
    val map = mutableMapOf<String, String>()
    keys().forEach { key ->
        map[key] = getString(key)
    }
    return map
}

data class PluginStatistics(
    val pluginId: String,
    val totalCount: Int,
    val startDate: Instant,
    val endDate: Instant,
    val latestDataPoint: DataPoint?
)
