package com.domain.app.core.plugin.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.domain.app.core.plugin.PluginCapability
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Extension property for DataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "plugin_permissions")

/**
 * Manages plugin permissions and security policies
 */
@Singleton
class PluginPermissionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore
    
    // Preference keys
    private fun pluginPermissionsKey(pluginId: String) = stringSetPreferencesKey("plugin_permissions_$pluginId")
    private fun pluginDeniedTimeKey(pluginId: String) = longPreferencesKey("plugin_denied_time_$pluginId")
    private fun pluginGrantedByKey(pluginId: String) = stringPreferencesKey("plugin_granted_by_$pluginId")
    
    /**
     * Grant permissions to a plugin
     */
    suspend fun grantPermissions(
        pluginId: String,
        permissions: Set<PluginCapability>,
        grantedBy: String = "user"
    ) {
        dataStore.edit { preferences ->
            val key = pluginPermissionsKey(pluginId)
            val currentPermissions = preferences[key] ?: emptySet()
            preferences[key] = currentPermissions + permissions.map { it.name }
            
            // Record who granted the permissions
            preferences[pluginGrantedByKey(pluginId)] = grantedBy
            preferences[pluginDeniedTimeKey(pluginId)] = 0L
        }
    }
    
    /**
     * Revoke permissions from a plugin
     */
    suspend fun revokePermissions(
        pluginId: String,
        permissions: Set<PluginCapability>
    ) {
        dataStore.edit { preferences ->
            val key = pluginPermissionsKey(pluginId)
            val currentPermissions = preferences[key] ?: emptySet()
            preferences[key] = currentPermissions - permissions.map { it.name }
            
            // If all permissions revoked, mark as denied
            if (preferences[key]?.isEmpty() == true) {
                preferences[pluginDeniedTimeKey(pluginId)] = System.currentTimeMillis()
                preferences.remove(pluginGrantedByKey(pluginId))
            }
        }
    }
    
    /**
     * Revoke all permissions from a plugin
     */
    suspend fun revokeAllPermissions(pluginId: String) {
        dataStore.edit { preferences ->
            preferences.remove(pluginPermissionsKey(pluginId))
            preferences.remove(pluginGrantedByKey(pluginId))
            preferences[pluginDeniedTimeKey(pluginId)] = System.currentTimeMillis()
        }
    }
    
    /**
     * Overload for backwards compatibility
     */
    suspend fun revokePermissions(pluginId: String) {
        revokeAllPermissions(pluginId)
    }
    
    /**
     * Check if a plugin has a specific capability
     */
    suspend fun hasCapability(pluginId: String, capability: PluginCapability): Boolean {
        return dataStore.data.map { preferences ->
            val permissions = preferences[pluginPermissionsKey(pluginId)] ?: emptySet()
            permissions.contains(capability.name)
        }.first()
    }
    
    /**
     * Check if a plugin has a permission string (backwards compatibility)
     */
    suspend fun hasPermission(pluginId: String, permission: String): Boolean {
        return try {
            val capability = PluginCapability.valueOf(permission)
            hasCapability(pluginId, capability)
        } catch (e: IllegalArgumentException) {
            false
        }
    }
    
    /**
     * Get all granted capabilities for a plugin
     */
    suspend fun getGrantedCapabilities(pluginId: String): Set<PluginCapability> {
        return dataStore.data.map { preferences ->
            val permissions = preferences[pluginPermissionsKey(pluginId)] ?: emptySet()
            permissions.mapNotNull { permissionName ->
                try {
                    PluginCapability.valueOf(permissionName)
                } catch (e: IllegalArgumentException) {
                    null
                }
            }.toSet()
        }.first()
    }
    
    /**
     * Get granted permissions (backwards compatibility)
     */
    suspend fun getGrantedPermissions(pluginId: String): Set<PluginCapability> {
        return getGrantedCapabilities(pluginId)
    }
    
    /**
     * Get all plugins with specific capability
     */
    suspend fun getPluginsWithCapability(capability: PluginCapability): Set<String> {
        return dataStore.data.map { preferences ->
            preferences.asMap()
                .filterKeys { key ->
                    key is Preferences.Key<*> && key.name.startsWith("plugin_permissions_")
                }
                .filter { (_, value) ->
                    (value as? Set<*>)?.contains(capability.name) == true
                }
                .map { (key, _) ->
                    key.name.removePrefix("plugin_permissions_")
                }
                .toSet()
        }.first()
    }
    
    /**
     * Get all plugin permissions (for UI display)
     */
    fun getAllPluginPermissions(): Flow<Map<String, Set<PluginCapability>>> {
        return dataStore.data.map { preferences ->
            preferences.asMap()
                .filterKeys { key ->
                    key is Preferences.Key<*> && key.name.startsWith("plugin_permissions_")
                }
                .mapNotNull { (key, value) ->
                    val pluginId = key.name.removePrefix("plugin_permissions_")
                    val permissions = (value as? Set<*>)?.mapNotNull { permission ->
                        try {
                            PluginCapability.valueOf(permission.toString())
                        } catch (e: IllegalArgumentException) {
                            null
                        }
                    }?.toSet() ?: emptySet()
                    
                    if (permissions.isNotEmpty()) {
                        pluginId to permissions
                    } else {
                        null
                    }
                }
                .toMap()
        }
    }
    
    /**
     * Clear all permission data (for testing/reset)
     */
    suspend fun clearAllPermissions() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
