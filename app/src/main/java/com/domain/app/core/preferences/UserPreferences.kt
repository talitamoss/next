// app/src/main/java/com/domain/app/core/preferences/UserPreferences.kt
package com.domain.app.core.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Extension property to get DataStore instance
private val Context.userPreferencesDataStore by preferencesDataStore(
    name = "user_preferences"
)

/**
 * User preferences management class that provides a clean interface
 * for ViewModels to access and modify user settings.
 * 
 * This class acts as a bridge between the SettingsViewModel and the
 * underlying preference storage mechanisms (DataStore and PreferencesManager).
 */
@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager
) {
    
    // DataStore instance for user-specific preferences
    private val dataStore = context.userPreferencesDataStore
    
    // Preference keys
    companion object {
        // User Profile
        private val KEY_USER_NAME = stringPreferencesKey("user_name")
        private val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        private val KEY_USER_AVATAR_URI = stringPreferencesKey("user_avatar_uri")
        
        // UI Preferences
        private val KEY_DARK_MODE = booleanPreferencesKey("dark_mode_enabled")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode") // "light", "dark", "system"
        private val KEY_LANGUAGE = stringPreferencesKey("app_language")
        private val KEY_FONT_SIZE = stringPreferencesKey("font_size") // "small", "medium", "large"
        
        // Security
        private val KEY_BIOMETRIC_AUTH = booleanPreferencesKey("biometric_auth_enabled")
        private val KEY_AUTO_LOCK_TIMEOUT = intPreferencesKey("auto_lock_timeout_minutes")
        private val KEY_REQUIRE_AUTH_ON_START = booleanPreferencesKey("require_auth_on_start")
        
        // Privacy
        private val KEY_ANALYTICS = booleanPreferencesKey("analytics_enabled")
        private val KEY_CRASH_REPORTING = booleanPreferencesKey("crash_reporting_enabled")
        private val KEY_PERSONALIZED_ADS = booleanPreferencesKey("personalized_ads_enabled")
        
        // Notifications
        private val KEY_NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        private val KEY_NOTIFICATION_SOUND = booleanPreferencesKey("notification_sound_enabled")
        private val KEY_NOTIFICATION_VIBRATE = booleanPreferencesKey("notification_vibrate_enabled")
        
        // Backup
        private val KEY_AUTO_BACKUP = booleanPreferencesKey("auto_backup_enabled")
        private val KEY_BACKUP_FREQUENCY = stringPreferencesKey("backup_frequency")
        private val KEY_BACKUP_WIFI_ONLY = booleanPreferencesKey("backup_wifi_only")
        private val KEY_LAST_BACKUP_TIME = longPreferencesKey("last_backup_timestamp")
        private val KEY_BACKUP_LOCATION = stringPreferencesKey("backup_location")
        
        // App State
        private val KEY_FIRST_LAUNCH = booleanPreferencesKey("first_launch_completed")
        private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val KEY_APP_VERSION = stringPreferencesKey("last_app_version")
    }
    
    // ========== USER PROFILE PREFERENCES ==========
    
    /**
     * Flow that emits the current user name
     */
    val userName: Flow<String?> = dataStore.data
        .map { preferences ->
            preferences[KEY_USER_NAME]
        }
    
    /**
     * Flow that emits the current user email
     */
    val userEmail: Flow<String?> = dataStore.data
        .map { preferences ->
            preferences[KEY_USER_EMAIL]
        }
    
    /**
     * Flow that emits the user avatar URI
     */
    val userAvatarUri: Flow<String?> = dataStore.data
        .map { preferences ->
            preferences[KEY_USER_AVATAR_URI]
        }
    
    /**
     * Set the user name
     */
    suspend fun setUserName(name: String?) {
        dataStore.edit { preferences ->
            if (name != null) {
                preferences[KEY_USER_NAME] = name
            } else {
                preferences.remove(KEY_USER_NAME)
            }
        }
    }
    
    /**
     * Set the user email
     */
    suspend fun setUserEmail(email: String?) {
        dataStore.edit { preferences ->
            if (email != null) {
                preferences[KEY_USER_EMAIL] = email
            } else {
                preferences.remove(KEY_USER_EMAIL)
            }
        }
    }
    
    /**
     * Set the user avatar URI
     */
    suspend fun setUserAvatarUri(uri: String?) {
        dataStore.edit { preferences ->
            if (uri != null) {
                preferences[KEY_USER_AVATAR_URI] = uri
            } else {
                preferences.remove(KEY_USER_AVATAR_URI)
            }
        }
    }
    
    // ========== USER INTERFACE PREFERENCES ==========
    
    /**
     * Flow that emits the current dark mode state
     */
    val isDarkMode: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[KEY_DARK_MODE] ?: false
        }
    
    /**
     * Flow that emits the current theme mode (light/dark/system)
     */
    val themeMode: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[KEY_THEME_MODE] ?: "system"
        }
    
    /**
     * Flow that emits the current app language
     */
    val appLanguage: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[KEY_LANGUAGE] ?: "en"
        }
    
    /**
     * Flow that emits the current font size preference
     */
    val fontSize: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[KEY_FONT_SIZE] ?: "medium"
        }
    
    /**
     * Set the dark mode preference
     */
    suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_DARK_MODE] = enabled
            // If explicitly setting dark mode, update theme mode too
            preferences[KEY_THEME_MODE] = if (enabled) "dark" else "light"
        }
    }
    
    /**
     * Set the theme mode (light/dark/system)
     */
    suspend fun setThemeMode(mode: String) {
        dataStore.edit { preferences ->
            preferences[KEY_THEME_MODE] = mode
            // Update dark mode flag based on theme mode
            when (mode) {
                "dark" -> preferences[KEY_DARK_MODE] = true
                "light" -> preferences[KEY_DARK_MODE] = false
                // "system" - let the system decide, don't change dark mode flag
            }
        }
    }
    
    /**
     * Set the app language
     */
    suspend fun setAppLanguage(languageCode: String) {
        dataStore.edit { preferences ->
            preferences[KEY_LANGUAGE] = languageCode
        }
    }
    
    /**
     * Set the font size preference
     */
    suspend fun setFontSize(size: String) {
        dataStore.edit { preferences ->
            preferences[KEY_FONT_SIZE] = size
        }
    }
    
    // ========== SECURITY PREFERENCES ==========
    
    /**
     * Flow that emits the current biometric authentication enabled state
     */
    val biometricAuthEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[KEY_BIOMETRIC_AUTH] ?: false
        }
    
    /**
     * Flow that emits the auto-lock timeout in minutes
     */
    val autoLockTimeout: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[KEY_AUTO_LOCK_TIMEOUT] ?: 5 // Default 5 minutes
        }
    
    /**
     * Flow that emits whether auth is required on app start
     */
    val requireAuthOnStart: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[KEY_REQUIRE_AUTH_ON_START] ?: false
        }
    
    /**
     * Set the biometric authentication enabled preference
     */
    suspend fun setBiometricAuthEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_BIOMETRIC_AUTH] = enabled
        }
    }
    
    /**
     * Set the auto-lock timeout in minutes
     */
    suspend fun setAutoLockTimeout(minutes: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_AUTO_LOCK_TIMEOUT] = minutes
        }
    }
    
    /**
     * Set whether auth is required on app start
     */
    suspend fun setRequireAuthOnStart(required: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_REQUIRE_AUTH_ON_START] = required
        }
    }
    
    // ========== PRIVACY PREFERENCES ==========
    
    /**
     * Flow that emits the current analytics enabled state
     */
    val analyticsEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[KEY_ANALYTICS] ?: false
        }
    
    /**
     * Flow that emits the current crash reporting enabled state
     */
    val crashReportingEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[KEY_CRASH_REPORTING] ?: true
        }
    
    /**
     * Set the analytics enabled preference
     */
    suspend fun setAnalyticsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_ANALYTICS] = enabled
        }
    }
    
    /**
     * Set the crash reporting enabled preference
     */
    suspend fun setCrashReportingEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_CRASH_REPORTING] = enabled
        }
    }
    
    // ========== NOTIFICATION PREFERENCES ==========
    
    /**
     * Flow that emits the current notifications enabled state
     */
    val notificationsEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[KEY_NOTIFICATIONS] ?: true
        }
    
    /**
     * Flow that emits whether notification sound is enabled
     */
    val notificationSoundEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[KEY_NOTIFICATION_SOUND] ?: true
        }
    
    /**
     * Flow that emits whether notification vibration is enabled
     */
    val notificationVibrateEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[KEY_NOTIFICATION_VIBRATE] ?: true
        }
    
    /**
     * Set the notifications enabled preference
     */
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_NOTIFICATIONS] = enabled
        }
    }
    
    /**
     * Set the notification sound preference
     */
    suspend fun setNotificationSoundEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_NOTIFICATION_SOUND] = enabled
        }
    }
    
    /**
     * Set the notification vibration preference
     */
    suspend fun setNotificationVibrateEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_NOTIFICATION_VIBRATE] = enabled
        }
    }
    
    // ========== BACKUP PREFERENCES ==========
    
    /**
     * Flow that emits the current auto backup enabled state
     */
    val autoBackupEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[KEY_AUTO_BACKUP] ?: false
        }
    
    /**
     * Flow that emits the current backup frequency
     */
    val backupFrequency: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[KEY_BACKUP_FREQUENCY] ?: "daily"
        }
    
    /**
     * Flow that emits whether backups should only happen on WiFi
     */
    val backupWifiOnly: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[KEY_BACKUP_WIFI_ONLY] ?: true
        }
    
    /**
     * Flow that emits the last backup timestamp
     */
    val lastBackupTime: Flow<Long> = dataStore.data
        .map { preferences ->
            preferences[KEY_LAST_BACKUP_TIME] ?: 0L
        }
    
    /**
     * Flow that emits the backup location
     */
    val backupLocation: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[KEY_BACKUP_LOCATION] ?: "Local storage"
        }
    
    /**
     * Set the auto backup enabled preference
     */
    suspend fun setAutoBackupEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_AUTO_BACKUP] = enabled
        }
    }
    
    /**
     * Set the backup frequency (daily/weekly/monthly)
     */
    suspend fun setBackupFrequency(frequency: String) {
        dataStore.edit { preferences ->
            preferences[KEY_BACKUP_FREQUENCY] = frequency
        }
    }
    
    /**
     * Set whether backups should only happen on WiFi
     */
    suspend fun setBackupWifiOnly(wifiOnly: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_BACKUP_WIFI_ONLY] = wifiOnly
        }
    }
    
    /**
     * Set the last backup timestamp
     */
    suspend fun setLastBackupTime(timestamp: Long) {
        dataStore.edit { preferences ->
            preferences[KEY_LAST_BACKUP_TIME] = timestamp
        }
    }
    
    /**
     * Set the backup location
     */
    suspend fun setBackupLocation(location: String) {
        dataStore.edit { preferences ->
            preferences[KEY_BACKUP_LOCATION] = location
        }
    }
    
    // ========== DASHBOARD PREFERENCES (delegated to PreferencesManager) ==========
    
    /**
     * Flow that emits the list of plugin IDs shown on the dashboard
     */
    val dashboardPlugins: Flow<List<String>> = preferencesManager.dashboardPlugins
    
    /**
     * Update the dashboard plugins
     */
    suspend fun updateDashboardPlugins(pluginIds: List<String>) {
        preferencesManager.updateDashboardPlugins(pluginIds)
    }
    
    /**
     * Add a plugin to the dashboard
     */
    suspend fun addPluginToDashboard(pluginId: String) {
        preferencesManager.addToDashboard(pluginId)
    }
    
    /**
     * Remove a plugin from the dashboard
     */
    suspend fun removePluginFromDashboard(pluginId: String) {
        preferencesManager.removeFromDashboard(pluginId)
    }
    
    /**
     * Check if a plugin is on the dashboard
     */
    fun isPluginOnDashboard(pluginId: String): Flow<Boolean> {
        return preferencesManager.isOnDashboard(pluginId)
    }
    
    /**
     * Get the number of plugins on dashboard
     */
    fun getDashboardPluginCount(): Flow<Int> {
        return preferencesManager.getDashboardPluginCount()
    }
    
    // ========== APP STATE PREFERENCES ==========
    
    /**
     * Check if this is the first time the app is launched
     */
val isFirstLaunch: Flow<Boolean> = dataStore.data
    .map { preferences ->
        preferences[KEY_FIRST_LAUNCH] != true
        }
    
    /**
     * Check if onboarding has been completed
     */
    val isOnboardingCompleted: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[KEY_ONBOARDING_COMPLETED] ?: false
        }
    
    /**
     * Mark that the initial setup has been completed
     */
    suspend fun markFirstLaunchComplete() {
        dataStore.edit { preferences ->
            preferences[KEY_FIRST_LAUNCH] = true
        }
    }
    
    /**
     * Mark that onboarding has been completed
     */
    suspend fun markOnboardingComplete() {
        dataStore.edit { preferences ->
            preferences[KEY_ONBOARDING_COMPLETED] = true
        }
    }
    
    /**
     * Update the last known app version
     */
    suspend fun updateAppVersion(version: String) {
        dataStore.edit { preferences ->
            preferences[KEY_APP_VERSION] = version
        }
    }
    
    // ========== UTILITY FUNCTIONS ==========
    
    /**
     * Clear all user preferences (useful for logout/reset)
     */
    suspend fun clearAllPreferences() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
        // Also clear dashboard plugins
        updateDashboardPlugins(emptyList())
    }
    
    /**
     * Export all preferences as a map (for backup)
     */
    suspend fun exportPreferences(): Map<String, Any?> {
        val prefs = mutableMapOf<String, Any?>()
        dataStore.data.collect { preferences ->
            preferences.asMap().forEach { (key, value) ->
                prefs[key.name] = value
            }
        }
        return prefs
    }
}
