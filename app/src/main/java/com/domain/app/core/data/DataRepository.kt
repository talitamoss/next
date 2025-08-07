package com.domain.app.core.data

import com.domain.app.core.database.dao.DataPointDao
import com.domain.app.core.database.entities.DataPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import java.time.LocalDateTime
import java.time.ZoneOffset

@Singleton
class DataRepository @Inject constructor(
    private val dataPointDao: DataPointDao
) {
    // Core CRUD operations
    suspend fun insertDataPoint(dataPoint: DataPoint) {
        dataPointDao.insert(dataPoint)
    }

    suspend fun updateDataPoint(dataPoint: DataPoint) {
        dataPointDao.update(dataPoint)
    }

    suspend fun deleteDataPoint(dataPoint: DataPoint) {
        dataPointDao.delete(dataPoint)
    }

    fun getDataPointById(id: Long): Flow<DataPoint?> {
        return dataPointDao.getById(id)
    }

    fun getDataPointsByPlugin(pluginId: String): Flow<List<DataPoint>> {
        return dataPointDao.getByPluginId(pluginId)
    }

    fun getAllDataPoints(): Flow<List<DataPoint>> {
        return dataPointDao.getAll()
    }

    fun getDataPointsInRange(
        pluginId: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): Flow<List<DataPoint>> {
        val startMillis = startTime.toInstant(ZoneOffset.UTC).toEpochMilli()
        val endMillis = endTime.toInstant(ZoneOffset.UTC).toEpochMilli()
        return dataPointDao.getByPluginIdInRange(pluginId, startMillis, endMillis)
    }

    // Compatibility methods - Expected by UI layer
    fun getRecentData(pluginId: String, limit: Int = 10): Flow<List<DataPoint>> {
        return dataPointDao.getRecentByPluginId(pluginId, limit)
    }

    suspend fun getDataCount(pluginId: String): Int {
        return dataPointDao.getCountByPluginId(pluginId)
    }

    fun searchDataPoints(query: String): Flow<List<DataPoint>> {
        return dataPointDao.search("%$query%")
    }

    // Batch operations
    suspend fun insertDataPoints(dataPoints: List<DataPoint>) {
        dataPointDao.insertAll(dataPoints)
    }

    suspend fun deleteDataPoints(dataPoints: List<DataPoint>) {
        dataPointDao.deleteAll(dataPoints)
    }

    suspend fun deleteAllDataForPlugin(pluginId: String) {
        dataPointDao.deleteByPluginId(pluginId)
    }

    // Statistics and aggregations
    fun getDataCountFlow(pluginId: String): Flow<Int> {
        return flow {
            emit(dataPointDao.getCountByPluginId(pluginId))
        }
    }

    fun getLatestDataPoint(pluginId: String): Flow<DataPoint?> {
        return dataPointDao.getLatestByPluginId(pluginId)
    }

    // Export/Import support
    suspend fun getAllDataPointsForExport(): List<DataPoint> {
        return dataPointDao.getAllForExport()
    }

    suspend fun clearAllData() {
        dataPointDao.deleteAll()
    }
}
