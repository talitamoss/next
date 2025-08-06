// app/src/main/java/com/domain/app/core/data/DataRepository.kt
package com.domain.app.core.data

import com.domain.app.core.storage.encryption.EncryptionManager
import com.domain.app.core.storage.dao.DataPointDao
import com.domain.app.core.storage.entity.DataPointEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing data points with encryption support
 * 
 * NOTE: Currently using simple string serialization for JSON fields.
 * TODO: Add proper JSON library (Gson or kotlinx.serialization) to dependencies
 */
@Singleton
class DataRepository @Inject constructor(
    private val dataPointDao: DataPointDao,
    private val encryptionManager: EncryptionManager
) {
    
    /**
     * Save a new data point
     */
    suspend fun saveDataPoint(dataPoint: DataPoint) {
        try {
            val entity = dataPoint.toEntity()
            dataPointDao.insert(entity)
        } catch (e: Exception) {
            throw DataRepositoryException("Failed to save data point", e)
        }
    }
    
    /**
     * Save multiple data points in a transaction
     */
    suspend fun saveDataPoints(dataPoints: List<DataPoint>) {
        try {
            val entities = dataPoints.map { it.toEntity() }
            dataPointDao.insertAll(entities)
        } catch (e: Exception) {
            throw DataRepositoryException("Failed to save data points", e)
        }
    }
    
    /**
     * Get a specific data point by ID
     */
    suspend fun getDataPoint(id: String): DataPoint? {
        return try {
            dataPointDao.getById(id)?.toDomainModel()
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Delete a specific data point by ID
     */
    suspend fun deleteDataPoint(id: String) {
        try {
            dataPointDao.deleteById(id)
        } catch (e: Exception) {
            throw DataRepositoryException("Failed to delete data point", e)
        }
    }
    
    /**
     * Delete a data point entity
     */
    suspend fun deleteDataPoint(dataPoint: DataPoint) {
        try {
            val entity = dataPoint.toEntity()
            dataPointDao.delete(entity)
        } catch (e: Exception) {
            throw DataRepositoryException("Failed to delete data point", e)
        }
    }
    
    /**
     * Delete multiple data points
     */
    suspend fun deleteDataPoints(ids: List<String>) {
        try {
            dataPointDao.deleteByIds(ids)
        } catch (e: Exception) {
            throw DataRepositoryException("Failed to delete data points", e)
        }
    }
    
    /**
     * Get latest data points with limit
     */
    fun getLatestDataPoints(limit: Int = 100): Flow<List<DataPoint>> {
        return dataPointDao.getLatestDataPoints(limit)
            .map { entities ->
                entities.mapNotNull { entity ->
                    try {
                        entity.toDomainModel()
                    } catch (e: Exception) {
                        null // Skip corrupted entries
                    }
                }
            }
    }
    
    /**
     * Get recent data within the last N hours
     * Used by DashboardViewModel and ExportManager
     */
    fun getRecentData(hours: Int): Flow<List<DataPoint>> {
        val since = Instant.now().minus(hours.toLong(), ChronoUnit.HOURS)
        return dataPointDao.getDataSince(since)
            .map { entities ->
                entities.mapNotNull { entity ->
                    try {
                        entity.toDomainModel()
                    } catch (e: Exception) {
                        null
                    }
                }
            }
    }
    
    /**
     * Get all data points for a specific plugin
     */
    fun getPluginData(pluginId: String): Flow<List<DataPoint>> {
        return dataPointDao.getPluginData(pluginId)
            .map { entities ->
                entities.mapNotNull { entity ->
                    try {
                        entity.toDomainModel()
                    } catch (e: Exception) {
                        null
                    }
                }
            }
    }
    
    /**
     * Get plugin data within a time range (for ExportManager)
     */
    suspend fun getPluginDataInRange(
        pluginId: String,
        startTime: Instant,
        endTime: Instant
    ): List<DataPoint> {
        return try {
            dataPointDao.getPluginDataInRange(pluginId, startTime, endTime)
                .map { entities ->
                    entities.mapNotNull { entity ->
                        try {
                            entity.toDomainModel()
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
                .flow.collect { dataPoints ->
                    return dataPoints
                }
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Get data points within a time range
     */
    fun getDataPointsInRange(
        startTime: Instant,
        endTime: Instant,
        pluginId: String? = null
    ): Flow<List<DataPoint>> {
        val flow = if (pluginId != null) {
            dataPointDao.getPluginDataInRange(pluginId, startTime, endTime)
        } else {
            dataPointDao.getDataInRange(startTime, endTime)
        }
        
        return flow.map { entities ->
            entities.mapNotNull { entity ->
                try {
                    entity.toDomainModel()
                } catch (e: Exception) {
                    null
                }
            }
        }
    }
    
    /**
     * Get data count for a plugin (used by SecureDataRepository)
     */
    suspend fun getDataCount(pluginId: String): Int {
        return try {
            dataPointDao.getPluginDataCount(pluginId)
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * Search data points
     */
    fun searchDataPoints(query: String): Flow<List<DataPoint>> {
        val searchQuery = "%$query%"
        return dataPointDao.searchDataPoints(searchQuery)
            .map { entities ->
                entities.mapNotNull { entity ->
                    try {
                        entity.toDomainModel()
                    } catch (e: Exception) {
                        null
                    }
                }
            }
    }
    
    /**
     * Get all data points (use with caution)
     */
    suspend fun getAllDataPoints(): List<DataPoint> {
        return try {
            dataPointDao.getAllDataPoints()
                .mapNotNull { entity ->
                    try {
                        entity.toDomainModel()
                    } catch (e: Exception) {
                        null
                    }
                }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Get statistics for a plugin
     */
    suspend fun getPluginStatistics(pluginId: String): DataStatistics {
        val count = dataPointDao.getPluginDataCount(pluginId)
        val latest = dataPointDao.getLatestPluginEntry(pluginId)
        val oldest = dataPointDao.getOldestPluginEntry(pluginId)
        
        return DataStatistics(
            totalCount = count,
            latestTimestamp = latest?.timestamp,
            oldestTimestamp = oldest?.timestamp,
            pluginId = pluginId
        )
    }
    
    /**
     * Update sync status for data points
     */
    suspend fun markAsSynced(ids: List<String>) {
        try {
            dataPointDao.updateSyncStatusBatch(ids, true)
        } catch (e: Exception) {
            throw DataRepositoryException("Failed to update sync status", e)
        }
    }
    
    /**
     * Get unsynced data points
     */
    suspend fun getUnsyncedData(): List<DataPoint> {
        return try {
            dataPointDao.getDataBySyncStatus(false)
                .mapNotNull { entity ->
                    try {
                        entity.toDomainModel()
                    } catch (e: Exception) {
                        null
                    }
                }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Clean old data (used by SettingsViewModel)
     */
    suspend fun cleanOldData(before: Instant) {
        try {
            dataPointDao.deleteOlderThan(before)
        } catch (e: Exception) {
            throw DataRepositoryException("Failed to clean old data", e)
        }
    }
    
    /**
     * Clean up old data based on retention days (alternative signature)
     */
    suspend fun cleanupOldData(retentionDays: Int) {
        try {
            if (retentionDays == 0) {
                // Delete all data
                dataPointDao.deleteAll()
            } else {
                val cutoffTime = Instant.now().minus(retentionDays.toLong(), ChronoUnit.DAYS)
                dataPointDao.deleteOlderThan(cutoffTime)
            }
        } catch (e: Exception) {
            throw DataRepositoryException("Failed to cleanup old data", e)
        }
    }
    
    /**
     * Get count of all data points
     */
    suspend fun getTotalDataPointCount(): Int {
        return dataPointDao.getTotalCount()
    }
    
    /**
     * Export all data points (unencrypted for export)
     */
    suspend fun getAllDataPointsForExport(): List<DataPoint> {
        return try {
            dataPointDao.getAllDataPoints().mapNotNull { entity ->
                try {
                    entity.toDomainModel()
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            throw DataRepositoryException("Failed to export data points", e)
        }
    }
    
    /**
     * Export plugin data points (unencrypted for export)
     */
    suspend fun getPluginDataPointsForExport(pluginId: String): List<DataPoint> {
        return try {
            dataPointDao.getAllPluginData(pluginId).mapNotNull { entity ->
                try {
                    entity.toDomainModel()
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            throw DataRepositoryException("Failed to export plugin data", e)
        }
    }
    
    // Extension functions for mapping between domain and entity models
    
    /**
     * Convert DataPoint to DataPointEntity
     * Maps the domain model to the database entity structure
     */
    private fun DataPoint.toEntity(): DataPointEntity {
        return DataPointEntity(
            id = id,
            pluginId = pluginId,
            timestamp = timestamp,
            type = type,
            // Simple serialization - convert Map to string representation
            // Format: "key1=value1;key2=value2"
            valueJson = value.entries.joinToString(";") { "${it.key}=${it.value}" },
            metadataJson = metadata?.entries?.joinToString(";") { "${it.key}=${it.value}" },
            source = source,
            version = version,
            synced = false, // New data is unsynced by default
            createdAt = timestamp, // Use timestamp as createdAt
            extra1 = null,
            extra2 = null,
            extra3 = null,
            extra4 = null
        )
    }
    
    /**
     * Convert DataPointEntity to DataPoint
     * Maps the database entity to the domain model
     */
    private fun DataPointEntity.toDomainModel(): DataPoint {
        return DataPoint(
            id = id,
            pluginId = pluginId,
            timestamp = timestamp,
            type = type,
            value = parseSimpleMap(valueJson),
            metadata = metadataJson?.let { parseSimpleStringMap(it) },
            source = source,
            version = version
        )
    }
    
    /**
     * Parse simple key=value string to Map<String, Any>
     * Format: "key1=value1;key2=value2"
     */
    private fun parseSimpleMap(str: String): Map<String, Any> {
        return try {
            if (str.isEmpty()) return emptyMap()
            
            str.split(";")
                .filter { it.contains("=") }
                .associate { entry ->
                    val parts = entry.split("=", limit = 2)
                    val key = parts[0]
                    val value = parts.getOrNull(1) ?: ""
                    // Try to parse as number, otherwise keep as string
                    key to (value.toDoubleOrNull() ?: value.toIntOrNull() ?: value)
                }
        } catch (e: Exception) {
            emptyMap()
        }
    }
    
    /**
     * Parse simple key=value string to Map<String, String>
     */
    private fun parseSimpleStringMap(str: String): Map<String, String> {
        return try {
            if (str.isEmpty()) return emptyMap()
            
            str.split(";")
                .filter { it.contains("=") }
                .associate { entry ->
                    val parts = entry.split("=", limit = 2)
                    parts[0] to (parts.getOrNull(1) ?: "")
                }
        } catch (e: Exception) {
            emptyMap()
        }
    }
}

/**
 * Custom exception for repository operations
 */
class DataRepositoryException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Data statistics model
 */
data class DataStatistics(
    val totalCount: Int,
    val latestTimestamp: Instant?,
    val oldestTimestamp: Instant?,
    val pluginId: String
)
