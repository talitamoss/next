package com.domain.app.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore
    
    companion object {
        private val DASHBOARD_PLUGINS_KEY = stringSetPreferencesKey("dashboard_plugins")
        private val DASHBOARD_ORDER_KEY = stringPreferencesKey("dashboard_order")
        private const val MAX_DASHBOARD_PLUGINS = 6
    }
    
    /**
     * Get the list of plugin IDs that should be shown on the dashboard
     */
    val dashboardPlugins: Flow<List<String>> = dataStore.data
        .map { preferences ->
            val pluginsSet = preferences[DASHBOARD_PLUGINS_KEY] ?: emptySet()
            val orderString = preferences[DASHBOARD_ORDER_KEY] ?: ""
            
            if (orderString.isNotEmpty()) {
                // Return in saved order
                orderString.split(",").filter { it in pluginsSet }
            } else {
                // Return in default order
                pluginsSet.toList()
            }
        }
    
    /**
     * Update the dashboard plugins
     */
    suspend fun updateDashboardPlugins(pluginIds: List<String>) {
        dataStore.edit { preferences ->
            // Limit to maximum allowed
            val limitedIds = pluginIds.take(MAX_DASHBOARD_PLUGINS)
            
            preferences[DASHBOARD_PLUGINS_KEY] = limitedIds.toSet()
            preferences[DASHBOARD_ORDER_KEY] = limitedIds.joinToString(",")
        }
    }
    
    /**
     * Add a plugin to the dashboard
     */
    suspend fun addToDashboard(pluginId: String) {
        dataStore.edit { preferences ->
            val current = preferences[DASHBOARD_PLUGINS_KEY] ?: emptySet()
            if (current.size < MAX_DASHBOARD_PLUGINS) {
                preferences[DASHBOARD_PLUGINS_KEY] = current + pluginId
                
                // Update order
                val currentOrder = preferences[DASHBOARD_ORDER_KEY]?.split(",") ?: emptyList()
                preferences[DASHBOARD_ORDER_KEY] = (currentOrder + pluginId).joinToString(",")
            }
        }
    }
    
    /**
     * Remove a plugin from the dashboard
     */
    suspend fun removeFromDashboard(pluginId: String) {
        dataStore.edit { preferences ->
            val current = preferences[DASHBOARD_PLUGINS_KEY] ?: emptySet()
            preferences[DASHBOARD_PLUGINS_KEY] = current - pluginId
            
            // Update order
            val currentOrder = preferences[DASHBOARD_ORDER_KEY]?.split(",") ?: emptyList()
            preferences[DASHBOARD_ORDER_KEY] = currentOrder.filter { it != pluginId }.joinToString(",")
        }
    }
    
    /**
     * Check if a plugin is on the dashboard
     */
    fun isOnDashboard(pluginId: String): Flow<Boolean> {
        return dashboardPlugins.map { it.contains(pluginId) }
    }
    
    /**
     * Get the number of plugins on dashboard
     */
    fun getDashboardPluginCount(): Flow<Int> {
        return dashboardPlugins.map { it.size }
    }
}
