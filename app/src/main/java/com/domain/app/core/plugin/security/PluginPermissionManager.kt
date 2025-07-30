package com.domain.app.core.plugin.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
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
 * 
 * File location: app/src/main/java/com/domain/app/core/plugin/security/PluginPermissionManager.kt
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
        capabilities: Set<PluginCapability>,
        grantedBy: String = "user"
    ) {
        dataStore.edit { preferences ->
            val key = pluginPermissionsKey(pluginId)
            val currentPermissions = preferences[key] ?: emptySet()
            preferences[key] = currentPermissions + capabilities.map { it.name }
            
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
        capabilities: Set<PluginCapability>
    ) {
        dataStore.edit { preferences ->
            val key = pluginPermissionsKey(pluginId)
            val currentPermissions = preferences[key] ?: emptySet()
            preferences[key] = currentPermissions - capabilities.map { it.name }
            
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
     * Check if a plugin has a specific capability
     */
    suspend fun hasCapability(pluginId: String, capability: PluginCapability): Boolean {
        return dataStore.data.map { preferences ->
            val permissions = preferences[pluginPermissionsKey(pluginId)] ?: emptySet()
            permissions.contains(capability.name)
        }.first()
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
     * Get all plugins with specific capability
     */
    suspend fun getPluginsWithCapability(capability: PluginCapability): Set<String> {
        return dataStore.data.map { preferences ->
            preferences.asMap()
                .filterKeys { it is Preferences.Key<Set<String>> && it.name.startsWith("plugin_permissions_") }
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
     * Clear all permission data (for testing/reset)
     */
    suspend fun clearAllPermissions() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
