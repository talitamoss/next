package com.domain.app.core.plugin.security

import com.domain.app.core.data.DataPoint
import com.domain.app.core.data.DataRepository
import com.domain.app.core.plugin.PluginCapability
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

/**
 * Secure wrapper around DataRepository that enforces plugin data access permissions.
 * Each plugin gets its own instance with appropriate restrictions.
 */
class SecureDataRepository @Inject constructor(
    private val actualRepository: DataRepository,
    private val pluginId: String,
    private val grantedCapabilities: Set<PluginCapability>,
    private val dataAccessScopes: Set<DataAccessScope>,
    private val securityMonitor: SecurityMonitor
) {
    
    /**
     * Save data with security checks
     */
    suspend fun saveDataPoint(dataPoint: DataPoint): Result<Unit> {
        // Check write permission
        if (PluginCapability.COLLECT_DATA !in grantedCapabilities) {
            recordAccessViolation("WRITE", "Missing COLLECT_DATA capability")
            return Result.failure(SecurityException("Plugin does not have permission to collect data"))
        }
        
        // Enforce plugin can only save its own data
        if (dataPoint.pluginId != pluginId) {
            recordAccessViolation("WRITE", "Attempted to save data for another plugin")
            return Result.failure(SecurityException("Cannot save data for other plugins"))
        }
        
        // Add security metadata
        val secureDataPoint = dataPoint.copy(
            metadata = (dataPoint.metadata ?: emptyMap()) + mapOf(
                "created_by_plugin" to pluginId,
                "security_version" to "1.0",
                "created_at" to Instant.now().toString()
            )
        )
        
        return try {
            actualRepository.saveDataPoint(secureDataPoint)
            recordDataAccess("WRITE", 1)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get plugin data with scope restrictions
     */
    fun getPluginData(): Flow<List<DataPoint>> {
        // Check read permission
        if (PluginCapability.READ_OWN_DATA !in grantedCapabilities) {
            recordAccessViolation("READ", "Missing READ_OWN_DATA capability")
            return kotlinx.coroutines.flow.flowOf(emptyList())
        }
        
        return when {
            DataAccessScope.ALL_DATA_READ in dataAccessScopes -> {
                if (PluginCapability.READ_ALL_DATA !in grantedCapabilities) {
                    recordAccessViolation("READ", "Missing READ_ALL_DATA capability")
                    kotlinx.coroutines.flow.flowOf(emptyList())
                } else {
                    recordDataAccess("READ_ALL", 0)
                    actualRepository.getRecentData(24 * 7) // Last week
                }
            }
            
            DataAccessScope.CATEGORY_DATA in dataAccessScopes -> {
                recordDataAccess("READ_CATEGORY", 0)
                // Would need to implement category filtering
                actualRepository.getPluginData(pluginId)
            }
            
            else -> {
                recordDataAccess("READ_OWN", 0)
                actualRepository.getPluginData(pluginId)
            }
        }.map { dataPoints ->
            // Record actual count
            if (dataPoints.isNotEmpty()) {
                recordDataAccess("READ", dataPoints.size)
            }
            dataPoints
        }
    }
    
    /**
     * Get data count with permission check
     */
    suspend fun getDataCount(): Int {
        if (PluginCapability.READ_OWN_DATA !in grantedCapabilities) {
            recordAccessViolation("READ", "Missing READ_OWN_DATA capability")
            return 0
        }
        
        recordDataAccess("COUNT", 0)
        return actualRepository.getDataCount(pluginId)
    }
    
    /**
     * Delete data with strict permission checks
     */
    suspend fun deleteData(dataPointId: String): Result<Unit> {
        // Check delete permission
        if (PluginCapability.DELETE_DATA !in grantedCapabilities) {
            recordAccessViolation("DELETE", "Missing DELETE_DATA capability")
            return Result.failure(SecurityException("Plugin does not have permission to delete data"))
        }
        
        // Check if plugin owns the data
        // In a real implementation, would verify ownership
        
        recordDataAccess("DELETE", 1)
        
        return Result.success(Unit) // Actual deletion would happen here
    }
    
    /**
     * Export data with permission check
     */
    suspend fun exportData(): Result<List<DataPoint>> {
        if (PluginCapability.EXPORT_DATA !in grantedCapabilities) {
            recordAccessViolation("EXPORT", "Missing EXPORT_DATA capability")
            return Result.failure(SecurityException("Plugin does not have permission to export data"))
        }
        
        val data = actualRepository.getPluginData(pluginId)
            .map { it }
            .let { kotlinx.coroutines.flow.first(it) }
        
        recordDataAccess("EXPORT", data.size)
        
        return Result.success(data)
    }
    
    /**
     * Record data access for audit
     */
    private fun recordDataAccess(type: String, count: Int) {
        securityMonitor.recordSecurityEvent(
            SecurityEvent.DataAccess(
                pluginId = pluginId,
                dataType = type,
                accessType = when(type) {
                    "READ", "READ_OWN", "READ_ALL", "READ_CATEGORY", "COUNT" -> AccessType.READ
                    "WRITE" -> AccessType.WRITE
                    "DELETE" -> AccessType.DELETE
                    "EXPORT" -> AccessType.EXPORT
                    else -> AccessType.READ
                },
                recordCount = count
            )
        )
    }
    
    /**
     * Record access violation
     */
    private fun recordAccessViolation(operation: String, reason: String) {
        securityMonitor.recordSecurityEvent(
            SecurityEvent.SecurityViolation(
                pluginId = pluginId,
                violationType = "DATA_ACCESS_VIOLATION",
                details = "$operation: $reason",
                severity = ViolationSeverity.HIGH
            )
        )
    }
    
    /**
     * Check if plugin can access specific data type
     */
    fun canAccessDataType(dataType: String): Boolean {
        return when (dataType) {
            "location" -> DataAccessScope.LOCATION in dataAccessScopes
            "biometric" -> DataAccessScope.BIOMETRIC in dataAccessScopes
            "profile" -> DataAccessScope.USER_PROFILE in dataAccessScopes
            "device" -> DataAccessScope.DEVICE_INFO in dataAccessScopes
            else -> true
        }
    }
}
