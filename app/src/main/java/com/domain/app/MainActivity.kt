package com.domain.app.core.plugin.security

import com.domain.app.core.data.DataPoint
import com.domain.app.core.data.DataRepository
import com.domain.app.core.plugin.PluginCapability
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Secure wrapper around DataRepository that enforces plugin permissions.
 * This class provides a security layer between plugins and the actual data repository,
 * ensuring that plugins can only access data they have permission to access.
 */
class SecureDataRepository(
    private val actualRepository: DataRepository,
    private val pluginId: String,
    private val grantedCapabilities: Set<PluginCapability>,
    private val dataAccessScopes: Set<DataAccessScope>,
    private val securityMonitor: SecurityMonitor
) {
    private val permissionManager = object {
        fun hasCapability(pluginId: String, capability: PluginCapability): Boolean {
            return grantedCapabilities.contains(capability)
        }
    }
    
    /**
     * Save data point with security checks
     */
    suspend fun saveDataPoint(dataPoint: DataPoint): Result<Unit> {
        // Verify plugin has permission to collect data
        if (!permissionManager.hasCapability(pluginId, PluginCapability.COLLECT_DATA)) {
            securityMonitor.recordSecurityEvent(
                SecurityEvent.SecurityViolation(
                    pluginId = pluginId,
                    violationType = "UNAUTHORIZED_DATA_SAVE",
                    details = "Attempted to save data without COLLECT_DATA permission",
                    severity = ViolationSeverity.MEDIUM
                )
            )
            return Result.failure(SecurityException("Plugin $pluginId lacks COLLECT_DATA permission"))
        }
        
        // Verify plugin is only saving its own data
        if (dataPoint.pluginId != pluginId) {
            securityMonitor.recordSecurityEvent(
                SecurityEvent.SecurityViolation(
                    pluginId = pluginId,
                    violationType = "CROSS_PLUGIN_DATA_SAVE",
                    details = "Attempted to save data for another plugin: ${dataPoint.pluginId}",
                    severity = ViolationSeverity.HIGH
                )
            )
            return Result.failure(SecurityException("Plugin can only save its own data"))
        }
        
        return try {
            actualRepository.saveDataPoint(dataPoint)
            securityMonitor.recordSecurityEvent(
                SecurityEvent.DataAccess(
                    pluginId = pluginId,
                    dataType = dataPoint.type,
                    accessType = AccessType.WRITE,
                    recordCount = 1
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get plugin's own data with security checks
     */
    fun getPluginData(targetPluginId: String): Flow<List<DataPoint>> = flow {
        // Check if plugin can read the requested data
        val canRead = when {
            // Plugin reading its own data
            pluginId == targetPluginId -> 
                permissionManager.hasCapability(pluginId, PluginCapability.READ_OWN_DATA)
            
            // Plugin reading another plugin's data
            else -> 
                permissionManager.hasCapability(pluginId, PluginCapability.READ_ALL_DATA)
        }
        
        if (!canRead) {
            val capability = if (pluginId == targetPluginId) {
                PluginCapability.READ_OWN_DATA
            } else {
                PluginCapability.READ_ALL_DATA
            }
            
            securityMonitor.recordSecurityEvent(
                SecurityEvent.SecurityViolation(
                    pluginId = pluginId,
                    violationType = "UNAUTHORIZED_DATA_READ",
                    details = "Attempted to read data from plugin: $targetPluginId without $capability",
                    severity = ViolationSeverity.MEDIUM
                )
            )
            
            throw SecurityException("Plugin $pluginId lacks permission to read data")
        }
        
        // Log access and return data
        emitAll(actualRepository.getPluginData(targetPluginId).map { dataPoints ->
            securityMonitor.recordSecurityEvent(
                SecurityEvent.DataAccess(
                    pluginId = pluginId,
                    dataType = "plugin_data",
                    accessType = AccessType.READ,
                    recordCount = dataPoints.size
                )
            )
            dataPoints
        })
    }
    
    /**
     * Get data count with security checks
     */
    suspend fun getPluginDataCount(targetPluginId: String): Int {
        // Check permissions (same logic as getPluginData)
        val canRead = when {
            pluginId == targetPluginId -> 
                permissionManager.hasCapability(pluginId, PluginCapability.READ_OWN_DATA)
            else -> 
                permissionManager.hasCapability(pluginId, PluginCapability.READ_ALL_DATA)
        }
        
        if (!canRead) {
            return 0
        }
        
        return actualRepository.getPluginDataCount(targetPluginId)
    }
    
    /**
     * Delete data with security checks
     */
    suspend fun deletePluginData(dataPointIds: List<String>): Result<Unit> {
        if (!permissionManager.hasCapability(pluginId, PluginCapability.DELETE_DATA)) {
            securityMonitor.recordSecurityEvent(
                SecurityEvent.SecurityViolation(
                    pluginId = pluginId,
                    violationType = "UNAUTHORIZED_DATA_DELETE",
                    details = "Attempted to delete ${dataPointIds.size} data points",
                    severity = ViolationSeverity.HIGH
                )
            )
            return Result.failure(SecurityException("Plugin $pluginId lacks DELETE_DATA permission"))
        }
        
        // TODO: Verify plugin owns all the data points it's trying to delete
        // This would require fetching the data points first to check their pluginId
        
        return try {
            // DataRepository doesn't have a delete method yet, so this is a placeholder
            // When implemented, it should delete the specified data points
            securityMonitor.recordSecurityEvent(
                SecurityEvent.DataAccess(
                    pluginId = pluginId,
                    dataType = "data_points",
                    accessType = AccessType.DELETE,
                    recordCount = dataPointIds.size
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get latest data points with security checks
     */
    fun getLatestDataPoints(limit: Int): Flow<List<DataPoint>> = flow {
        // Check if plugin has permission to read all data
        if (!permissionManager.hasCapability(pluginId, PluginCapability.READ_ALL_DATA)) {
            // If not, only return the plugin's own data
            if (permissionManager.hasCapability(pluginId, PluginCapability.READ_OWN_DATA)) {
                emitAll(actualRepository.getPluginData(pluginId))
            } else {
                throw SecurityException("Plugin $pluginId lacks permission to read data")
            }
        } else {
            // Plugin can read all data
            emitAll(actualRepository.getLatestDataPoints(limit).map { dataPoints ->
                securityMonitor.recordSecurityEvent(
                    SecurityEvent.DataAccess(
                        pluginId = pluginId,
                        dataType = "all_data",
                        accessType = AccessType.READ,
                        recordCount = dataPoints.size
                    )
                )
                dataPoints
            })
        }
    }
}
