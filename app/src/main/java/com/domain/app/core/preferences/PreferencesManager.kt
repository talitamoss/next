package com.domain.app.core.preferences

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
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

/**
 * Manages application preferences
 * 
 * File location: app/src/main/java/com/domain/app/core/preferences/PreferencesManager.kt
 */
@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore
    
    // Preference keys
    companion object {
        val ENABLED_PLUGINS = stringSetPreferencesKey("enabled_plugins")
        val SYNC_ON_STARTUP = booleanPreferencesKey("sync_on_startup")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val BACKUP_FREQUENCY = stringPreferencesKey("backup_frequency")
        val LAST_BACKUP = longPreferencesKey("last_backup")
    }
    
    /**
     * Get enabled plugins
     */
    val enabledPlugins: Flow<Set<String>> = dataStore.data.map { preferences ->
        preferences[ENABLED_PLUGINS] ?: emptySet()
    }
    
    /**
     * Add plugin to enabled list
     */
    suspend fun enablePlugin(pluginId: String) {
        dataStore.edit { preferences ->
            val current = preferences[ENABLED_PLUGINS] ?: emptySet()
            preferences[ENABLED_PLUGINS] = current + pluginId
        }
    }
    
    /**
     * Remove plugin from enabled list
     */
    suspend fun disablePlugin(pluginId: String) {
        dataStore.edit { preferences ->
            val current = preferences[ENABLED_PLUGINS] ?: emptySet()
            preferences[ENABLED_PLUGINS] = current - pluginId
        }
    }
    
    /**
     * Check if plugin is enabled
     */
    suspend fun isPluginEnabled(pluginId: String): Boolean {
        return dataStore.data.first()[ENABLED_PLUGINS]?.contains(pluginId) ?: false
    }
    
    /**
     * Get theme mode
     */
    val themeMode: Flow<String> = dataStore.data.map { preferences ->
        preferences[THEME_MODE] ?: "system"
    }
    
    /**
     * Set theme mode
     */
    suspend fun setThemeMode(mode: String) {
        dataStore.edit { preferences ->
            preferences[THEME_MODE] = mode
        }
    }
    
    /**
     * Get backup frequency
     */
    val backupFrequency: Flow<String> = dataStore.data.map { preferences ->
        preferences[BACKUP_FREQUENCY] ?: "weekly"
    }
    
    /**
     * Set backup frequency
     */
    suspend fun setBackupFrequency(frequency: String) {
        dataStore.edit { preferences ->
            preferences[BACKUP_FREQUENCY] = frequency
        }
    }
    
    /**
     * Update last backup time
     */
    suspend fun updateLastBackupTime() {
        dataStore.edit { preferences ->
            preferences[LAST_BACKUP] = System.currentTimeMillis()
        }
    }
    
    /**
     * Get last backup time
     */
    val lastBackupTime: Flow<Long> = dataStore.data.map { preferences ->
        preferences[LAST_BACKUP] ?: 0L
    }
    
    /**
     * Clear all preferences (for testing/reset)
     */
    suspend fun clearAll() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
