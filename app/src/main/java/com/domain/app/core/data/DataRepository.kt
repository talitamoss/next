package com.domain.app.core.data

import com.domain.app.core.storage.dao.DataPointDao
import com.domain.app.core.storage.entity.DataPointEntity
import com.domain.app.core.storage.encryption.EncryptionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

@Singleton
class DataRepository @Inject constructor(
    private val dataPointDao: DataPointDao,
    private val encryptionManager: EncryptionManager  // Added to match DI module
) {
    
    // ========== ENTITY CONVERSION HELPERS ==========
    
    /**
     * Convert DataPointEntity (database) to DataPoint (domain model)
     */
    private fun entityToDataPoint(entity: DataPointEntity): DataPoint {
        return DataPoint(
            id = entity.id,
            pluginId = entity.pluginId,
            timestamp = entity.timestamp,
            type = entity.type,
            value = parseJsonToMap(entity.valueJson),
            metadata = entity.metadataJson?.let { parseJsonToStringMap(it) },
            source = entity.source,
            version = entity.version
        )
    }
    
    /**
     * Convert DataPoint (domain model) to DataPointEntity (database)
     */
    private fun dataPointToEntity(dataPoint: DataPoint): DataPointEntity {
        return DataPointEntity(
            id = dataPoint.id,
            pluginId = dataPoint.pluginId,
            timestamp = dataPoint.timestamp,
            type = dataPoint.type,
            valueJson = mapToJson(dataPoint.value),
            metadataJson = dataPoint.metadata?.let { stringMapToJson(it) },
            source = dataPoint.source,
            version = dataPoint.version,
            synced = false,
            createdAt = Instant.now()
        )
    }
    
    /**
     * Parse JSON string to Map<String, Any>
     * TODO: Implement with proper JSON library (Gson/Moshi)
     */
    private fun parseJsonToMap(json: String): Map<String, Any> {
        return try {
            // Placeholder - implement with actual JSON parsing
            emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }
    
    /**
     * Parse JSON string to Map<String, String>
     * TODO: Implement with proper JSON library (Gson/Moshi)
     */
    private fun parseJsonToStringMap(json: String?): Map<String, String> {
        if (json == null) return emptyMap()
        return try {
            // Placeholder - implement with actual JSON parsing
            emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }
    
    /**
     * Convert Map to JSON string
     * TODO: Implement with proper JSON library (Gson/Moshi)
     */
    private fun mapToJson(map: Map<String, Any>): String {
        // Placeholder - implement with actual JSON serialization
        return "{}"
    }
    
    /**
     * Convert String Map to JSON string
     * TODO: Implement with proper JSON library (Gson/Moshi)
     */
    private fun stringMapToJson(map: Map<String, String>): String {
        // Placeholder - implement with actual JSON serialization
        return "{}"
    }
    
    // ========== CORE CRUD OPERATIONS ==========
    
    /**
     * Insert a new data point
     */
    suspend fun insertDataPoint(dataPoint: DataPoint) {
        val entity = dataPointToEntity(dataPoint)
        dataPointDao.insert(entity)
    }
    
    /**
     * Update an existing data point
     */
    suspend fun updateDataPoint(dataPoint: DataPoint) {
        val entity = dataPointToEntity(dataPoint)
        dataPointDao.update(entity)
    }
    
    /**
     * Delete a data point
     */
    suspend fun deleteDataPoint(dataPoint: DataPoint) {
        val entity = dataPointToEntity(dataPoint)
        dataPointDao.delete(entity)
    }
    
    /**
     * Delete a data point by ID (overload for UI convenience)
     */
    suspend fun deleteDataPoint(id: String) {
        dataPointDao.deleteById(id)
    }
    
    /**
     * Get a data point by ID
     */
    fun getDataPointById(id: String): Flow<DataPoint?> {
        return flow {
            val entity = dataPointDao.getById(id)
            emit(entity?.let { entityToDataPoint(it) })
        }
    }
    
    /**
     * Get all data points for a plugin
     */
    fun getDataPointsByPlugin(pluginId: String): Flow<List<DataPoint>> {
        return dataPointDao.getPluginData(pluginId).map { entities ->
            entities.map { entityToDataPoint(it) }
        }
    }
    
    /**
     * Get all data points
     */
    fun getAllDataPoints(): Flow<List<DataPoint>> {
        return flow {
            val entities = dataPointDao.getAllDataPoints()
            emit(entities.map { entityToDataPoint(it) })
        }
    }
    
    /**
     * Get data points in a time range
     */
    fun getDataPointsInRange(
        pluginId: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): Flow<List<DataPoint>> {
        val startInstant = startTime.toInstant(ZoneOffset.UTC)
        val endInstant = endTime.toInstant(ZoneOffset.UTC)
        return dataPointDao.getPluginDataInRange(pluginId, startInstant, endInstant)
            .map { entities -> entities.map { entityToDataPoint(it) } }
    }
    
    // ========== COMPATIBILITY METHODS FOR UI LAYER ==========
    
    /**
     * Get latest data points across all plugins
     * Required by: DataViewModel
     */
    fun getLatestDataPoints(limit: Int): Flow<List<DataPoint>> {
        return dataPointDao.getLatestDataPoints(limit).map { entities ->
            entities.map { entityToDataPoint(it) }
        }
    }
    
    /**
     * Get plugin data
     * Required by: DataViewModel, multiple other ViewModels
     */
    fun getPluginData(pluginId: String): Flow<List<DataPoint>> {
        return dataPointDao.getPluginData(pluginId).map { entities ->
            entities.map { entityToDataPoint(it) }
        }
    }

/**
 * Get ALL data points in a time range (not filtered by plugin)
 * Used by: ReflectViewModel for calendar view
 */
fun getDataInRange(
    startTime: Instant,
    endTime: Instant
): Flow<List<DataPoint>> {
    return dataPointDao.getDataInRange(startTime, endTime)
        .map { entities -> entities.map { entityToDataPoint(it) } }
}
    
    /**
     * Get recent data with limit (generic)
     * Required by: DashboardViewModel
     */
    fun getRecentData(limit: Int): Flow<List<DataPoint>> {
        return getLatestDataPoints(limit)
    }
    
    /**
     * Get recent data for a specific plugin
     * Compatibility method for older code
     */
    fun getRecentData(pluginId: String, limit: Int = 10): Flow<List<DataPoint>> {
        // Since DataPointDao doesn't have getRecentByPluginId, 
        // we'll use getPluginData and the UI can limit the display
        return getPluginData(pluginId)
    }
    
    /**
     * Get data count for a plugin
     */
    suspend fun getDataCount(pluginId: String): Int {
        return dataPointDao.getPluginDataCount(pluginId)
    }
    
    /**
     * Search data points
     */
    fun searchDataPoints(query: String): Flow<List<DataPoint>> {
        return dataPointDao.searchDataPoints("%$query%").map { entities ->
            entities.map { entityToDataPoint(it) }
        }
    }
    
    // ========== BATCH OPERATIONS ==========
    
    /**
     * Insert multiple data points
     */
    suspend fun insertDataPoints(dataPoints: List<DataPoint>) {
        val entities = dataPoints.map { dataPointToEntity(it) }
        dataPointDao.insertAll(entities)
    }
    
    /**
     * Delete multiple data points by object
     */
    suspend fun deleteDataPoints(dataPoints: List<DataPoint>) {
        val entities = dataPoints.map { dataPointToEntity(it) }
        dataPointDao.deleteAll(entities)
    }
    
    /**
     * Delete multiple data points by IDs
     * Required by: DataViewModel
     */
    suspend fun deleteDataPointsByIds(ids: List<String>) {
        dataPointDao.deleteByIds(ids)
    }
    
    /**
     * Delete all data for a plugin
     */
    suspend fun deleteAllDataForPlugin(pluginId: String) {
        dataPointDao.deleteByPluginId(pluginId)
    }
    
    // ========== STATISTICS AND AGGREGATIONS ==========
    
    /**
     * Get data count as a Flow
     */
    fun getDataCountFlow(pluginId: String): Flow<Int> {
        return flow {
            emit(dataPointDao.getPluginDataCount(pluginId))
        }
    }
    
    /**
     * Get the latest data point for a plugin
     */
    fun getLatestDataPoint(pluginId: String): Flow<DataPoint?> {
        return flow {
            val entity = dataPointDao.getLatestPluginEntry(pluginId)
            emit(entity?.let { entityToDataPoint(it) })
        }
    }
    
    // ========== EXPORT/IMPORT SUPPORT ==========
    
    /**
     * Get all data points for export
     */
    suspend fun getAllDataPointsForExport(): List<DataPoint> {
        val entities = dataPointDao.getAllDataPoints()
        return entities.map { entityToDataPoint(it) }
    }
    
    /**
     * Clear all data
     */
    suspend fun clearAllData() {
        dataPointDao.deleteAll()
    }
    
    // ========== METHODS NEEDED BY OTHER COMPONENTS ==========
    
    /**
     * Save a data point (alias for insertDataPoint)
     * Required by: SecureDataRepository, PluginManager
     */
    suspend fun saveDataPoint(dataPoint: DataPoint) {
        insertDataPoint(dataPoint)
    }
    
    /**
     * Get data in range (overload with Instant parameters)
     * Required by: ExportManager
     */
    fun getPluginDataInRange(
        pluginId: String,
        startTime: Instant,
        endTime: Instant
    ): Flow<List<DataPoint>> {
        return dataPointDao.getPluginDataInRange(pluginId, startTime, endTime)
            .map { entities -> entities.map { entityToDataPoint(it) } }
    }
}
