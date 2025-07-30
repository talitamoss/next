package com.domain.app.core.plugin.security

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.domain.app.core.plugin.PluginCapability
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.permissionDataStore by preferencesDataStore(name = "plugin_permissions")

@Singleton
class PluginPermissionManager @Inject constructor(
    private val context: Context,
    private val securityMonitor: SecurityMonitor
) {
    private val dataStore = context.permissionDataStore
    
    /**
     * Grant permissions to a plugin
     */
    suspend fun grantPermissions(
        pluginId: String,
        permissions: Set<PluginCapability>,
        grantedBy: String
    ) {
        dataStore.edit { prefs ->
            permissions.forEach { capability ->
                val key = stringSetPreferencesKey("${pluginId}_permissions")
                val current = prefs[key] ?: emptySet()
                prefs[key] = current + capability.name
                
                // Record security event
                securityMonitor.recordSecurityEvent(
                    SecurityEvent.PermissionGranted(
                        pluginId = pluginId,
                        capability = capability,
                        grantedBy = grantedBy
                    )
                )
            }
            
            // Store grant metadata
            prefs[stringPreferencesKey("${pluginId}_granted_by")] = grantedBy
            prefs[longPreferencesKey("${pluginId}_granted_at")] = System.currentTimeMillis()
        }
    }
    
    /**
     * Revoke all permissions for a plugin
     */
    suspend fun revokePermissions(pluginId: String) {
        dataStore.edit { prefs ->
            val key = stringSetPreferencesKey("${pluginId}_permissions")
            val revokedPermissions = prefs[key] ?: emptySet()
            
            prefs.remove(key)
            prefs.remove(stringPreferencesKey("${pluginId}_granted_by"))
            prefs.remove(longPreferencesKey("${pluginId}_granted_at"))
            
            // Record security events
            revokedPermissions.forEach { permissionName ->
                PluginCapability.values().find { it.name == permissionName }?.let { capability ->
                    securityMonitor.recordSecurityEvent(
                        SecurityEvent.PermissionDenied(
                            pluginId = pluginId,
                            capability = capability,
                            reason = "Permissions revoked by user"
                        )
                    )
                }
            }
        }
    }
    
    /**
     * Revoke specific permissions
     */
    suspend fun revokeSpecificPermissions(
        pluginId: String,
        permissions: Set<PluginCapability>
    ) {
        dataStore.edit { prefs ->
            val key = stringSetPreferencesKey("${pluginId}_permissions")
            val current = prefs[key] ?: emptySet()
            val permissionNames = permissions.map { it.name }.toSet()
            prefs[key] = current - permissionNames
            
            // Record security events
            permissions.forEach { capability ->
                securityMonitor.recordSecurityEvent(
                    SecurityEvent.PermissionDenied(
                        pluginId = pluginId,
                        capability = capability,
                        reason = "Permission revoked by user"
                    )
                )
            }
        }
    }
    
    /**
     * Check if plugin has a specific permission
     */
    suspend fun hasPermission(pluginId: String, capability: PluginCapability): Boolean {
        val key = stringSetPreferencesKey("${pluginId}_permissions")
        return dataStore.data.map { prefs ->
            prefs[key]?.contains(capability.name) ?: false
        }.first()
    }
    
    /**
     * Get all granted permissions for a plugin
     */
    suspend fun getGrantedPermissions(pluginId: String): Set<PluginCapability> {
        val key = stringSetPreferencesKey("${pluginId}_permissions")
        return dataStore.data.map { prefs ->
            val permissionNames = prefs[key] ?: emptySet()
            permissionNames.mapNotNull { name ->
                PluginCapability.values().find { it.name == name }
            }.toSet()
        }.first()
    }
    
    /**
     * Get granted permissions as a flow
     */
    fun getGrantedPermissionsFlow(pluginId: String): Flow<Set<PluginCapability>> {
        val key = stringSetPreferencesKey("${pluginId}_permissions")
        return dataStore.data.map { prefs ->
            val permissionNames = prefs[key] ?: emptySet()
            permissionNames.mapNotNull { name ->
                PluginCapability.values().find { it.name == name }
            }.toSet()
        }
    }
    
    /**
     * Get permission grant metadata
     */
    suspend fun getPermissionMetadata(pluginId: String): PermissionMetadata? {
        return dataStore.data.map { prefs ->
            val grantedBy = prefs[stringPreferencesKey("${pluginId}_granted_by")]
            val grantedAt = prefs[longPreferencesKey("${pluginId}_granted_at")]
            
            if (grantedBy != null && grantedAt != null) {
                PermissionMetadata(
                    pluginId = pluginId,
                    grantedBy = grantedBy,
                    grantedAt = grantedAt
                )
            } else {
                null
            }
        }.first()
    }
    
    /**
     * Check if any permissions are granted to a plugin
     */
    suspend fun hasAnyPermissions(pluginId: String): Boolean {
        val key = stringSetPreferencesKey("${pluginId}_permissions")
        return dataStore.data.map { prefs ->
            !prefs[key].isNullOrEmpty()
        }.first()
    }
    
    /**
     * Clear all permissions for all plugins (dangerous!)
     */
    suspend fun clearAllPermissions() {
        dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}

data class PermissionMetadata(
    val pluginId: String,
    val grantedBy: String,
    val grantedAt: Long
)
