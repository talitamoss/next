// app/src/main/java/com/domain/app/core/data/DataRepository.kt
package com.domain.app.core.data

import com.domain.app.core.storage.dao.DataPointDao
import com.domain.app.core.storage.entity.DataPointEntity
import com.domain.app.core.storage.encryption.EncryptionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
                    element.intOrNull != null -> element.int
                    element.longOrNull != null -> element.long
                    element.floatOrNull != null -> element.float
                    element.doubleOrNull != null -> element.double
                    else -> element.content
                }
            }
            is JsonArray -> {
                element.map { parseJsonValue(it) }
            }
            is JsonObject -> {
                element.entries.associate { (key, value) ->
                    key to parseJsonValue(value)
                }.filterValues { it != null }
            }
            is JsonNull -> null
        }
    }
    
    /**
     * Parse JSON string to Map<String, String>
     * FIXED: Properly implements JSON parsing for metadata
     */
    private fun parseJsonToStringMap(jsonString: String?): Map<String, String>? {
        if (jsonString == null || jsonString.isEmpty()) return null
        
        return try {
            val jsonElement = json.parseToJsonElement(jsonString)
            if (jsonElement is JsonObject) {
                jsonElement.entries.associate { (key, value) ->
                    key to when (value) {
                        is JsonPrimitive -> value.content
                        else -> value.toString()
                    }
                }
            } else {
                null
            }
        } catch (e: Exception) {
            println("Error parsing JSON to string map: ${e.message}")
            null
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
     * Convert Any value to JsonElement
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
     * Delete a data point by ID
     */
    suspend fun deleteDataPoint(id: String) {
        dataPointDao.deleteById(id)
    }
    
    /**
     * Get a single data point by ID
     */
    suspend fun getDataPoint(id: String): DataPoint? {
        return dataPointDao.getById(id)?.let { entityToDataPoint(it) }
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
