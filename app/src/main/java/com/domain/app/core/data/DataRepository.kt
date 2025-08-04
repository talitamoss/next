package com.domain.app.core.data

import android.util.Log
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
    
    companion object {
        private const val TAG = "DataRepository"
    }
    
    /**
     * Save a data point
     */
    suspend fun saveDataPoint(dataPoint: DataPoint) = withContext(Dispatchers.IO) {
        Log.d(TAG, "=== SAVING DATA POINT ===")
        Log.d(TAG, "Plugin ID: ${dataPoint.pluginId}")
        Log.d(TAG, "Type: ${dataPoint.type}")
        Log.d(TAG, "Value: ${dataPoint.value}")
        Log.d(TAG, "Timestamp: ${dataPoint.timestamp}")
        
        try {
            val entity = dataPoint.toEntity()
            Log.d(TAG, "Converted to entity:")
            Log.d(TAG, "  Entity ID: ${entity.id}")
            Log.d(TAG, "  Entity valueJson: ${entity.valueJson}")
            
            database.dataPointDao().insert(entity)
            Log.d(TAG, "Successfully inserted into database")
            
            // Verify insertion
            val count = database.dataPointDao().getCountByPlugin(dataPoint.pluginId)
            Log.d(TAG, "Plugin ${dataPoint.pluginId} now has $count total records")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save data point", e)
            throw e
        }
    }
    
    /**
     * Save multiple data points
     */
    suspend fun saveDataPoints(dataPoints: List<DataPoint>) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Saving ${dataPoints.size} data points")
        try {
            database.dataPointDao().insertAll(dataPoints.map { it.toEntity() })
            Log.d(TAG, "Successfully saved ${dataPoints.size} data points")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save data points", e)
            throw e
        }
    }
    
    /**
     * Get data for a specific plugin
     */
    fun getPluginData(pluginId: String): Flow<List<DataPoint>> {
        Log.d(TAG, "Getting data for plugin: $pluginId")
        return database.dataPointDao().getByPlugin(pluginId)
            .map { entities -> 
                Log.d(TAG, "Plugin $pluginId: found ${entities.size} entities")
                entities.map { it.toDataPoint() }
            }
    }
    
    /**
     * Get data for a plugin within a time range
     */
    suspend fun getPluginDataInRange(
        pluginId: String,
        start: Instant,
        end: Instant
    ): List<DataPoint> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Getting plugin $pluginId data from $start to $end")
        val entities = database.dataPointDao()
            .getByPluginAndTimeRange(pluginId, start, end)
        Log.d(TAG, "Found ${entities.size} entities in range")
        entities.map { it.toDataPoint() }
    }
    
    /**
     * Get recent data across all plugins
     */
    fun getRecentData(hours: Int = 24): Flow<List<DataPoint>> {
        val since = Instant.now().minus(hours.toLong(), ChronoUnit.HOURS)
        Log.d(TAG, "Getting recent data since: $since (last $hours hours)")
        
        return database.dataPointDao().getRecentData(since)
            .map { entities -> 
                Log.d(TAG, "Recent data query returned ${entities.size} entities")
                entities.forEach { entity ->
                    Log.d(TAG, "  Entity: ${entity.pluginId} at ${entity.timestamp}")
                }
                entities.map { it.toDataPoint() }
            }
    }
    
    /**
     * Get latest data points with limit
     */
    fun getLatestDataPoints(limit: Int): Flow<List<DataPoint>> {
        Log.d(TAG, "Getting latest $limit data points")
        return database.dataPointDao().getLatestDataPoints(limit)
            .map { entities -> 
                Log.d(TAG, "Latest query returned ${entities.size} entities")
                entities.map { it.toDataPoint() }
            }
    }
    
    /**
     * Get count of data points for a plugin
     */
    suspend fun getDataCount(pluginId: String): Int = withContext(Dispatchers.IO) {
        val count = database.dataPointDao().getCountByPlugin(pluginId)
        Log.d(TAG, "Plugin $pluginId has $count data points")
        count
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
    Log.d("DataConversion", "Converting DataPoint to Entity:")
    Log.d("DataConversion", "  ID: $id")
    Log.d("DataConversion", "  Value: $value")
    
    try {
        val valueJson = JSONObject(value).toString()
        Log.d("DataConversion", "  ValueJSON: $valueJson")
        
        return DataPointEntity(
            id = id,
            pluginId = pluginId,
            timestamp = timestamp,
            type = type,
            valueJson = valueJson,
            metadataJson = metadata?.let { JSONObject(it).toString() },
            source = source,
            version = version
        )
    } catch (e: Exception) {
        Log.e("DataConversion", "Failed to convert DataPoint to Entity", e)
        throw e
    }
}

fun DataPointEntity.toDataPoint(): DataPoint {
    try {
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
    } catch (e: Exception) {
        Log.e("DataConversion", "Failed to convert Entity to DataPoint", e)
        Log.e("DataConversion", "Entity valueJson: $valueJson")
        throw e
    }
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
