// app/src/main/java/com/domain/app/core/data/DataRepository.kt
package com.domain.app.core.data

import com.domain.app.core.security.EncryptionManager
import com.domain.app.core.storage.dao.DataPointDao
import com.domain.app.core.storage.entity.DataPointEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing data points with encryption support
 */
@Singleton
class DataRepository @Inject constructor(
    private val dataPointDao: DataPointDao,
    private val encryptionManager: EncryptionManager
) {
    /**
     * Save a new data point with encryption
     */
    suspend fun saveDataPoint(dataPoint: DataPoint) {
        try {
            val entity = dataPoint.toEntity()
            val encryptedEntity = encryptEntity(entity)
            dataPointDao.insert(encryptedEntity)
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
            val encryptedEntities = entities.map { encryptEntity(it) }
            dataPointDao.insertAll(encryptedEntities)
        } catch (e: Exception) {
            throw DataRepositoryException("Failed to save data points", e)
        }
    }
    
    /**
     * Get a specific data point by ID
     */
    suspend fun getDataPoint(id: String): DataPoint? {
        return try {
            dataPointDao.getById(id)?.let { entity ->
                decryptEntity(entity).toDomainModel()
            }
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
                        decryptEntity(entity).toDomainModel()
                    } catch (e: Exception) {
                        null // Skip corrupted entries
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
                        decryptEntity(entity).toDomainModel()
                    } catch (e: Exception) {
                        null
                    }
                }
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
        return if (pluginId != null) {
            dataPointDao.getPluginDataInRange(pluginId, startTime, endTime)
        } else {
            dataPointDao.getDataInRange(startTime, endTime)
        }.map { entities ->
            entities.mapNotNull { entity ->
                try {
                    decryptEntity(entity).toDomainModel()
                } catch (e: Exception) {
                    null
                }
            }
        }
    }
    
    /**
     * Get aggregated statistics for a plugin
     */
    suspend fun getPluginStatistics(pluginId: String): DataStatistics {
        val count = dataPointDao.getPluginDataCount(pluginId)
        val latestEntry = dataPointDao.getLatestPluginEntry(pluginId)
        val oldestEntry = dataPointDao.getOldestPluginEntry(pluginId)
        
        return DataStatistics(
            totalCount = count,
            latestTimestamp = latestEntry?.timestamp,
            oldestTimestamp = oldestEntry?.timestamp,
            pluginId = pluginId
        )
    }
    
    /**
     * Search data points by value content
     */
    fun searchDataPoints(query: String): Flow<List<DataPoint>> {
        return dataPointDao.searchDataPoints("%$query%")
            .map { entities ->
                entities.mapNotNull { entity ->
                    try {
                        val decrypted = decryptEntity(entity)
                        if (decrypted.value.contains(query, ignoreCase = true)) {
                            decrypted.toDomainModel()
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        null
                    }
                }
            }
    }
    
    /**
     * Clean up old data based on retention days
     */
    suspend fun cleanupOldData(retentionDays: Int) {
        try {
            if (retentionDays == 0) {
                // Delete all data
                dataPointDao.deleteAll()
            } else {
                val cutoffTime = Instant.now().minusSeconds(retentionDays * 24L * 60L * 60L)
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
                    decryptEntity(entity).toDomainModel()
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
                    decryptEntity(entity).toDomainModel()
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            throw DataRepositoryException("Failed to export plugin data", e)
        }
    }
    
    // Private helper methods
    
    private fun encryptEntity(entity: DataPointEntity): DataPointEntity {
        return entity.copy(
            value = encryptionManager.encrypt(entity.value),
            metadata = entity.metadata?.let { encryptionManager.encrypt(it) }
        )
    }
    
    private fun decryptEntity(entity: DataPointEntity): DataPointEntity {
        return entity.copy(
            value = encryptionManager.decrypt(entity.value),
            metadata = entity.metadata?.let { encryptionManager.decrypt(it) }
        )
    }
    
    private fun DataPoint.toEntity(): DataPointEntity {
        return DataPointEntity(
            id = id,
            pluginId = pluginId,
            timestamp = timestamp,
            value = value.toString(), // Convert map to JSON string
            metadata = metadata?.toString(),
            syncStatus = syncStatus.name,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
    
    private fun DataPointEntity.toDomainModel(): DataPoint {
        return DataPoint(
            id = id,
            pluginId = pluginId,
            timestamp = timestamp,
            value = parseJsonToMap(value),
            metadata = metadata?.let { parseJsonToMap(it) },
            syncStatus = SyncStatus.valueOf(syncStatus),
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
    
    private fun parseJsonToMap(json: String): Map<String, Any> {
        // Implementation would use actual JSON parsing
        // For now, simplified version
        return try {
            // Use Gson or Moshi to parse JSON
            emptyMap() // Placeholder
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
