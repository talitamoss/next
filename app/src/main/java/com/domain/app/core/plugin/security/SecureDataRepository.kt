package com.domain.app.core.plugin.security

import com.domain.app.core.data.DataPoint
import com.domain.app.core.data.DataRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Secure wrapper around DataRepository that enforces plugin permissions
 * 
 * File location: app/src/main/java/com/domain/app/core/plugin/security/SecureDataRepository.kt
 */
@Singleton
class SecureDataRepository @Inject constructor(
    private val dataRepository: DataRepository,
    private val permissionManager: PluginPermissionManager,
    private val securityMonitor: SecurityMonitor
) {
    
    /**
     * Save data point with security checks
     */
    suspend fun saveDataPoint(
        pluginId: String,
        dataPoint: DataPoint
    ): Result<Unit> {
        // Verify plugin has permission to collect data
        if (!permissionManager.hasCapability(pluginId, PluginCapability.COLLECT_DATA)) {
            securityMonitor.logViolation(
                pluginId = pluginId,
                capability = PluginCapability.COLLECT_DATA,
                action = "save_data_point",
                details = "Attempted to save data without COLLECT_DATA permission"
            )
            return Result.failure(SecurityException("Plugin $pluginId lacks COLLECT_DATA permission"))
        }
        
        // Verify plugin is only saving its own data
        if (dataPoint.pluginId != pluginId) {
            securityMonitor.logViolation(
                pluginId = pluginId,
                capability = PluginCapability.COLLECT_DATA,
                action = "save_data_point",
                details = "Attempted to save data for another plugin: ${dataPoint.pluginId}"
            )
            return Result.failure(SecurityException("Plugin can only save its own data"))
        }
        
        return try {
            dataRepository.saveDataPoint(dataPoint)
            securityMonitor.logDataAccess(pluginId, "save", 1)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get plugin's own data with security checks
     */
    fun getPluginData(
        requestingPluginId: String,
        targetPluginId: String
    ): Flow<List<DataPoint>> = flow {
        // Check if plugin can read the requested data
        val canRead = when {
            // Plugin reading its own data
            requestingPluginId == targetPluginId -> 
                permissionManager.hasCapability(requestingPluginId, PluginCapability.READ_OWN_DATA)
            
            // Plugin reading another plugin's data
            else -> 
                permissionManager.hasCapability(requestingPluginId, PluginCapability.READ_ALL_DATA)
        }
        
        if (!canRead) {
            val capability = if (requestingPluginId == targetPluginId) {
                PluginCapability.READ_OWN_DATA
            } else {
                PluginCapability.READ_ALL_DATA
            }
            
            securityMonitor.logViolation(
                pluginId = requestingPluginId,
                capability = capability,
                action = "read_data",
                details = "Attempted to read data from plugin: $targetPluginId"
            )
            
            throw SecurityException("Plugin $requestingPluginId lacks permission to read data")
        }
        
        // Log access and return data
        emitAll(dataRepository.getPluginData(targetPluginId).map { dataPoints ->
            securityMonitor.logDataAccess(requestingPluginId, "read", dataPoints.size)
            dataPoints
        })
    }
    
    /**
     * Get data count with security checks
     */
    suspend fun getPluginDataCount(
        requestingPluginId: String,
        targetPluginId: String
    ): Int {
        // Check permissions (same logic as getPluginData)
        val canRead = when {
            requestingPluginId == targetPluginId -> 
                permissionManager.hasCapability(requestingPluginId, PluginCapability.READ_OWN_DATA)
            else -> 
                permissionManager.hasCapability(requestingPluginId, PluginCapability.READ_ALL_DATA)
        }
        
        if (!canRead) {
            return 0
        }
        
        return dataRepository.getPluginDataCount(targetPluginId)
    }
    
    /**
     * Delete data with security checks
     */
    suspend fun deletePluginData(
        pluginId: String,
        dataPointIds: List<String>
    ): Result<Unit> {
        if (!permissionManager.hasCapability(pluginId, PluginCapability.DELETE_DATA)) {
            securityMonitor.logViolation(
                pluginId = pluginId,
                capability = PluginCapability.DELETE_DATA,
                action = "delete_data",
                details = "Attempted to delete ${dataPointIds.size} data points"
            )
            return Result.failure(SecurityException("Plugin $pluginId lacks DELETE_DATA permission"))
        }
        
        // TODO: Verify plugin owns all the data points it's trying to delete
        
        return try {
            // DataRepository doesn't have a delete method yet, so this is a placeholder
            securityMonitor.logDataAccess(pluginId, "delete", dataPointIds.size)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
