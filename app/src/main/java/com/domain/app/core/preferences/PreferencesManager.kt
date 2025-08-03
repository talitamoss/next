package com.domain.app.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.domain.app.core.theme.ThemeMode
import com.domain.app.ui.settings.BackupFrequency
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

/**
 * Manages app preferences using DataStore
 * Provides type-safe access to user preferences
 * 
 * File location: app/src/main/java/com/domain/app/core/preferences/PreferencesManager.kt
 */
@Singleton
class PreferencesManager @Inject constructor(
    private val context: Context
) {
    private val dataStore = context.dataStore
    
    companion object {
        private val THEME_KEY = stringPreferencesKey("theme")
        private val DASHBOARD_ENABLED_KEY = booleanPreferencesKey("dashboard_enabled")
        private val DASHBOARD_PLUGIN_IDS_KEY = stringSetPreferencesKey("dashboard_plugin_ids")
        private val ANALYTICS_ENABLED_KEY = booleanPreferencesKey("analytics_enabled")
        private val AUTO_BACKUP_ENABLED_KEY = booleanPreferencesKey("auto_backup_enabled")
        private val BACKUP_FREQUENCY_KEY = stringPreferencesKey("backup_frequency")
        private val ENCRYPTION_ENABLED_KEY = booleanPreferencesKey("encryption_enabled")
        private val BIOMETRIC_ENABLED_KEY = booleanPreferencesKey("biometric_enabled")
    }
    
    val preferences: Flow<AppPreferences> = dataStore.data.map { prefs ->
        AppPreferences(
            theme = ThemeMode.valueOf(prefs[THEME_KEY] ?: ThemeMode.SYSTEM.name),
            isDashboardEnabled = prefs[DASHBOARD_ENABLED_KEY] ?: true,
            dashboardPluginIds = prefs[DASHBOARD_PLUGIN_IDS_KEY] ?: emptySet(),
            isAnalyticsEnabled = prefs[ANALYTICS_ENABLED_KEY] ?: false,
            isAutoBackupEnabled = prefs[AUTO_BACKUP_ENABLED_KEY] ?: false,
            backupFrequency = BackupFrequency.valueOf(prefs[BACKUP_FREQUENCY_KEY] ?: BackupFrequency.WEEKLY.name),
            isEncryptionEnabled = prefs[ENCRYPTION_ENABLED_KEY] ?: true,
            isBiometricEnabled = prefs[BIOMETRIC_ENABLED_KEY] ?: false
        )
    }
    
    suspend fun setTheme(theme: ThemeMode) {
        dataStore.edit { prefs ->
            prefs[THEME_KEY] = theme.name
        }
    }
    
    suspend fun setDashboardEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[DASHBOARD_ENABLED_KEY] = enabled
        }
    }
    
    suspend fun getDashboardPluginIds(): Set<String> {
        return dataStore.data.map { prefs ->
            prefs[DASHBOARD_PLUGIN_IDS_KEY] ?: emptySet()
        }.first()
    }
    
    suspend fun setDashboardPluginIds(ids: Set<String>) {
        dataStore.edit { prefs ->
            prefs[DASHBOARD_PLUGIN_IDS_KEY] = ids
        }
    }
    
    suspend fun setAnalyticsEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[ANALYTICS_ENABLED_KEY] = enabled
        }
    }
    
    suspend fun setAutoBackupEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[AUTO_BACKUP_ENABLED_KEY] = enabled
        }
    }
    
    suspend fun setBackupFrequency(frequency: BackupFrequency) {
        dataStore.edit { prefs ->
            prefs[BACKUP_FREQUENCY_KEY] = frequency.name
        }
    }
    
    suspend fun setEncryptionEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[ENCRYPTION_ENABLED_KEY] = enabled
        }
    }
    
    suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[BIOMETRIC_ENABLED_KEY] = enabled
        }
    }
}

/**
 * App preferences data class
 */
data class AppPreferences(
    val theme: ThemeMode,
    val isDashboardEnabled: Boolean,
    val dashboardPluginIds: Set<String>,
    val isAnalyticsEnabled: Boolean,
    val isAutoBackupEnabled: Boolean,
    val backupFrequency: BackupFrequency,
    val isEncryptionEnabled: Boolean,
    val isBiometricEnabled: Boolean
)
