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
        val DASHBOARD_PLUGINS = stringSetPreferencesKey("dashboard_plugins")
        val FIRST_LAUNCH = booleanPreferencesKey("first_launch")
        val USER_NICKNAME = stringPreferencesKey("user_nickname")
        val ANALYTICS_ENABLED = booleanPreferencesKey("analytics_enabled")
        val CRASH_REPORTING_ENABLED = booleanPreferencesKey("crash_reporting_enabled")
    }
    
    /**
     * Get enabled plugins
     */
    val enabledPlugins: Flow<Set<String>> = dataStore.data.map { preferences ->
        preferences[ENABLED_PLUGINS] ?: emptySet()
    }
    
    /**
     * Get dashboard plugins
     */
    val dashboardPlugins: Flow<Set<String>> = dataStore.data.map { preferences ->
        preferences[DASHBOARD_PLUGINS] ?: emptySet()
    }
    
    /**
     * Get theme mode
     */
    val themeMode: Flow<String> = dataStore.data.map { preferences ->
        preferences[THEME_MODE] ?: "system"
    }
    
    /**
     * Get current theme (for compatibility)
     */
    val currentTheme: Flow<String> = themeMode
    
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
     * Add plugin to dashboard
     */
    suspend fun addToDashboard(pluginId: String) {
        dataStore.edit { preferences ->
            val current = preferences[DASHBOARD_PLUGINS] ?: emptySet()
            preferences[DASHBOARD_PLUGINS] = current + pluginId
        }
    }
    
    /**
     * Remove plugin from dashboard
     */
    suspend fun removeFromDashboard(pluginId: String) {
        dataStore.edit { preferences ->
            val current = preferences[DASHBOARD_PLUGINS] ?: emptySet()
            preferences[DASHBOARD_PLUGINS] = current - pluginId
        }
    }
    
    /**
     * Get dashboard plugin count
     */
    suspend fun getDashboardPluginCount(): Int {
        return dataStore.data.first()[DASHBOARD_PLUGINS]?.size ?: 0
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
     * Update theme (alias for setThemeMode)
     */
    suspend fun updateTheme(theme: String) = setThemeMode(theme)
    
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
     * Check if first launch
     */
    val isFirstLaunch: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[FIRST_LAUNCH] ?: true
    }
    
    /**
     * Mark first launch complete
     */
    suspend fun setFirstLaunchComplete() {
        dataStore.edit { preferences ->
            preferences[FIRST_LAUNCH] = false
        }
    }
    
    /**
     * Get user nickname
     */
    val userNickname: Flow<String> = dataStore.data.map { preferences ->
        preferences[USER_NICKNAME] ?: "User"
    }
    
    /**
     * Set user nickname
     */
    suspend fun setUserNickname(nickname: String) {
        dataStore.edit { preferences ->
            preferences[USER_NICKNAME] = nickname
        }
    }
    
    /**
     * Get analytics enabled
     */
    val analyticsEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[ANALYTICS_ENABLED] ?: false
    }
    
    /**
     * Set analytics enabled
     */
    suspend fun setAnalyticsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[ANALYTICS_ENABLED] = enabled
        }
    }
    
    /**
     * Get crash reporting enabled
     */
    val crashReportingEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[CRASH_REPORTING_ENABLED] ?: false
    }
    
    /**
     * Set crash reporting enabled
     */
    suspend fun setCrashReportingEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[CRASH_REPORTING_ENABLED] = enabled
        }
    }
    
    /**
     * Clear all preferences (for testing/reset)
     */
    suspend fun clearAll() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
    
    /**
     * Export all preferences as a map
     */
    suspend fun exportPreferences(): Map<String, Any?> {
        val preferences = dataStore.data.first()
        return mapOf(
            "enabled_plugins" to preferences[ENABLED_PLUGINS],
            "dashboard_plugins" to preferences[DASHBOARD_PLUGINS],
            "theme_mode" to preferences[THEME_MODE],
            "backup_frequency" to preferences[BACKUP_FREQUENCY],
            "last_backup" to preferences[LAST_BACKUP],
            "user_nickname" to preferences[USER_NICKNAME],
            "analytics_enabled" to preferences[ANALYTICS_ENABLED],
            "crash_reporting_enabled" to preferences[CRASH_REPORTING_ENABLED]
        )
    }
    
    /**
     * Import preferences from a map
     */
    suspend fun importPreferences(prefsMap: Map<String, Any?>) {
        dataStore.edit { preferences ->
            prefsMap["enabled_plugins"]?.let { 
                if (it is Set<*>) {
                    preferences[ENABLED_PLUGINS] = it.filterIsInstance<String>().toSet()
                }
            }
            prefsMap["dashboard_plugins"]?.let {
                if (it is Set<*>) {
                    preferences[DASHBOARD_PLUGINS] = it.filterIsInstance<String>().toSet()
                }
            }
            prefsMap["theme_mode"]?.let {
                if (it is String) {
                    preferences[THEME_MODE] = it
                }
            }
            prefsMap["backup_frequency"]?.let {
                if (it is String) {
                    preferences[BACKUP_FREQUENCY] = it
                }
            }
            prefsMap["last_backup"]?.let {
                if (it is Long) {
                    preferences[LAST_BACKUP] = it
                }
            }
            prefsMap["user_nickname"]?.let {
                if (it is String) {
                    preferences[USER_NICKNAME] = it
                }
            }
            prefsMap["analytics_enabled"]?.let {
                if (it is Boolean) {
                    preferences[ANALYTICS_ENABLED] = it
                }
            }
            prefsMap["crash_reporting_enabled"]?.let {
                if (it is Boolean) {
                    preferences[CRASH_REPORTING_ENABLED] = it
                }
            }
        }
    }
}
