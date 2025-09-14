// app/src/main/java/com/domain/app/core/data/DataRepository.kt
package com.domain.app.core.data

import com.domain.app.core.storage.dao.DataPointDao
import com.domain.app.core.storage.entity.DataPointEntity
import com.domain.app.core.storage.encryption.EncryptionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.*
import kotlinx.serialization.encodeToString
import javax.inject.Inject
import javax.inject.Singleton
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZoneId

@Singleton
class DataRepository @Inject constructor(
    private val dataPointDao: DataPointDao,
    private val encryptionManager: EncryptionManager
) {
    
    // JSON serializer instance
    private val json = Json { 
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }
    
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
     * FIXED: Properly implements JSON parsing
     */
    private fun parseJsonToMap(jsonString: String): Map<String, Any> {
        return try {
            if (jsonString.isEmpty()) return emptyMap()
            
            val jsonElement = json.parseToJsonElement(jsonString)
            parseJsonElement(jsonElement)
        } catch (e: Exception) {
            // Log error for debugging
            println("Error parsing JSON to map: ${e.message}")
            println("JSON string was: $jsonString")
            emptyMap()
        }
    }
    
    /**
     * Recursively parse JsonElement to Map/List/primitive types
     */
    private fun parseJsonElement(element: JsonElement): Map<String, Any> {
        return when (element) {
            is JsonObject -> {
                element.entries.associate { (key, value) ->
                    key to parseJsonValue(value)
                }.filterValues { it != null } as Map<String, Any>
            }
            else -> emptyMap()
        }
    }
    
    /**
     * Parse individual JSON values to Kotlin types
     */
    private fun parseJsonValue(element: JsonElement): Any? {
        return when (element) {
            is JsonPrimitive -> {
                when {
                    element.isString -> element.content
                    element.booleanOrNull != null -> element.boolean
                    element.longOrNull != null -> element.long
                    element.doubleOrNull != null -> element.double
                    else -> element.content
                }
            }
            is JsonObject -> {
                element.entries.associate { (key, value) ->
                    key to parseJsonValue(value)
                }.filterValues { it != null }
            }
            is JsonArray -> {
                element.map { parseJsonValue(it) }.filterNotNull()
            }
            is JsonNull -> null
        }
    }
    
    /**
     * Parse JSON string to Map<String, String>
     * Used for metadata field
     */
    private fun parseJsonToStringMap(jsonString: String): Map<String, String> {
        return try {
            json.decodeFromString<Map<String, String>>(jsonString)
        } catch (e: Exception) {
            emptyMap()
        }
    }
    
    /**
     * Convert Map<String, Any> to JSON string
     * FIXED: Properly implements JSON serialization
     */
    private fun mapToJson(map: Map<String, Any>): String {
        return try {
            val jsonObject = buildJsonObject {
                map.forEach { (key, value) ->
                    put(key, valueToJsonElement(value))
                }
            }
            json.encodeToString(jsonObject)
        } catch (e: Exception) {
            println("Error converting map to JSON: ${e.message}")
            "{}"
        }
    }
    
    /**
     * Convert any value to JsonElement
     */
    private fun valueToJsonElement(value: Any?): JsonElement {
        return when (value) {
            null -> JsonNull
            is String -> JsonPrimitive(value)
            is Number -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            is Map<*, *> -> {
                buildJsonObject {
                    value.forEach { (k, v) ->
                        if (k is String) {
                            put(k, valueToJsonElement(v))
                        }
                    }
                }
            }
            is List<*> -> {
                buildJsonArray {
                    value.forEach { item ->
                        add(valueToJsonElement(item))
                    }
                }
            }
            else -> JsonPrimitive(value.toString())
        }
    }
    
    /**
     * Convert Map<String, String> to JSON string
     * FIXED: Properly implements JSON serialization for metadata
     */
    private fun stringMapToJson(map: Map<String, String>): String {
        return try {
            json.encodeToString(map)
        } catch (e: Exception) {
            println("Error converting string map to JSON: ${e.message}")
            "{}"
        }
    }
    
    // ========== CORE OPERATIONS ==========
    
    /**
     * Save a data point
     */
    suspend fun saveDataPoint(dataPoint: DataPoint) {
        val entity = dataPointToEntity(dataPoint)
        dataPointDao.insert(entity)
    }
    
    /**
     * Insert a single data point (alias for saveDataPoint)
     * Required by: BackupManager, DashboardViewModel
     */
    suspend fun insertDataPoint(dataPoint: DataPoint) {
        saveDataPoint(dataPoint)
    }
    
    /**
     * Delete a data point by ID
     */
    suspend fun deleteDataPoint(id: String) {
        dataPointDao.deleteById(id)
    }
    
    /**
     * Delete multiple data points by IDs
     * Required by: DataViewModel
     */
    suspend fun deleteDataPointsByIds(ids: List<String>) {
        ids.forEach { id ->
            dataPointDao.deleteById(id)
        }
    }
    
    /**
     * Clear all data from the database
     * Required by: BackupManager, SettingsViewModel, DataManagementViewModel
     */
    suspend fun clearAllData() {
        dataPointDao.deleteAll()
    }
    
    /**
     * Alias for clearAllData() - required by DataManagementViewModel
     * VERIFICATION: This is OUTSIDE clearAllData function, as a separate function
     */
    suspend fun deleteAllData() = clearAllData()
    
    /**
     * Get a single data point by ID
     */
    suspend fun getDataPoint(id: String): DataPoint? {
        return dataPointDao.getById(id)?.let { entityToDataPoint(it) }
    }
    
    /**
     * Get recent data as Flow (for ViewModels that use Flow operations)
     * Required by: DashboardViewModel, DataViewModel (they call .filter{}.collect{})
     * VERIFIED: Returns Flow for use with .filter{}.collect{}
     */
    fun getRecentData(limit: Int = 100): Flow<List<DataPoint>> {
        return dataPointDao.getLatestDataPoints(limit).map { entities ->
            entities.map { entityToDataPoint(it) }
        }
    }
    
    /**
     * Get recent data as List (suspend version for direct access)
     * Required by: ExportManager (needs List, but needs to make its function suspend)
     * Required by: SecureDataRepository (needs to wrap in Result or make suspend)
     */
    suspend fun getRecentDataList(limit: Int = 100): List<DataPoint> {
        return dataPointDao.getLatestDataPoints(limit).first().map { entityToDataPoint(it) }
    }
    
    /**
     * Get total count of all data points
     * Required by: SecureDataRepository (but it passes pluginId!)
     */
    suspend fun getDataCount(): Int {
        return dataPointDao.getTotalCount()
    }
    
    /**
     * Get count for specific plugin
     * Required by: SecureDataRepository.kt:108 (calls with pluginId)
     */
    suspend fun getDataCount(pluginId: String): Int {
        return dataPointDao.getPluginDataCount(pluginId)
    }
    
    /**
     * Search data points by query
     * Required by: DataViewModel (expects Flow)
     * VERIFIED: Returns Flow as expected
     */
    fun searchDataPoints(query: String): Flow<List<DataPoint>> {
        return dataPointDao.searchDataPoints("%$query%").map { entities ->
            entities.map { entityToDataPoint(it) }
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
     * Get data points in a time range (Flow version)
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
    
    /**
     * Get plugin data in a specific time range (suspend version)
     * Required by: ExportManager.kt:103 (needs suspend returning List)
     * VERIFIED: Returns List<DataPoint> as expected
     */
    suspend fun getPluginDataInRange(
        pluginId: String,
        startTime: Instant,
        endTime: Instant
    ): List<DataPoint> {
        return dataPointDao.getPluginDataInRange(pluginId, startTime, endTime)
            .first()
            .map { entityToDataPoint(it) }
    }
    
    // ========== COMPATIBILITY METHODS FOR UI LAYER ==========
    
    /**
     * Get latest data points across all plugins
     */
    fun getLatestDataPoints(limit: Int): Flow<List<DataPoint>> {
        return dataPointDao.getLatestDataPoints(limit).map { entities ->
            entities.map { entityToDataPoint(it) }
        }
    }
    
    /**
     * Get plugin data
     */
    fun getPluginData(pluginId: String): Flow<List<DataPoint>> {
        return dataPointDao.getPluginData(pluginId).map { entities ->
            entities.map { entityToDataPoint(it) }
        }
    }

    /**
     * Get ALL data points in a time range (not filtered by plugin)
     */
    fun getDataInRange(
        startTime: Instant,
        endTime: Instant
    ): Flow<List<DataPoint>> {
        return dataPointDao.getDataInRange(startTime, endTime).map { entities ->
            entities.map { entityToDataPoint(it) }
        }
    }
    
    /**
     * Get all data for export
     */
    suspend fun getAllDataForExport(): List<DataPoint> {
        return dataPointDao.getAllDataPoints().map { entityToDataPoint(it) }
    }
    
    /**
     * Get plugin data for export
     */
    suspend fun getPluginDataForExport(pluginId: String): List<DataPoint> {
        return dataPointDao.getAllPluginData(pluginId).map { entityToDataPoint(it) }
    }
}
