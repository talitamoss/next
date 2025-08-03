package com.domain.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domain.app.core.preferences.PreferencesManager
import com.domain.app.core.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Settings screen
 * Manages app preferences including theme, dashboard visibility, and other settings
 * 
 * File location: app/src/main/java/com/domain/app/ui/settings/SettingsViewModel.kt
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        loadSettings()
    }
    
    /**
     * Load current settings from preferences
     */
    private fun loadSettings() {
        viewModelScope.launch {
            preferencesManager.preferences.collect { prefs ->
                _uiState.value = _uiState.value.copy(
                    currentTheme = prefs.theme,
                    isDashboardEnabled = prefs.isDashboardEnabled,
                    isAnalyticsEnabled = prefs.isAnalyticsEnabled,
                    isAutoBackupEnabled = prefs.isAutoBackupEnabled,
                    backupFrequency = prefs.backupFrequency,
                    isEncryptionEnabled = prefs.isEncryptionEnabled,
                    isBiometricEnabled = prefs.isBiometricEnabled
                )
            }
        }
    }
    
    /**
     * Set the app theme
     * Persists the theme preference and applies it immediately
     */
    fun setTheme(theme: ThemeMode) {
        viewModelScope.launch {
            try {
                preferencesManager.setTheme(theme)
                _uiState.value = _uiState.value.copy(currentTheme = theme)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to change theme: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Toggle dashboard visibility
     * Controls whether the dashboard is shown as the home screen
     */
    fun toggleDashboard() {
        viewModelScope.launch {
            try {
                val newState = !_uiState.value.isDashboardEnabled
                preferencesManager.setDashboardEnabled(newState)
                _uiState.value = _uiState.value.copy(isDashboardEnabled = newState)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to toggle dashboard: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Toggle analytics collection
     */
    fun toggleAnalytics() {
        viewModelScope.launch {
            try {
                val newState = !_uiState.value.isAnalyticsEnabled
                preferencesManager.setAnalyticsEnabled(newState)
                _uiState.value = _uiState.value.copy(isAnalyticsEnabled = newState)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to toggle analytics: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Toggle automatic backup
     */
    fun toggleAutoBackup() {
        viewModelScope.launch {
            try {
                val newState = !_uiState.value.isAutoBackupEnabled
                preferencesManager.setAutoBackupEnabled(newState)
                _uiState.value = _uiState.value.copy(isAutoBackupEnabled = newState)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to toggle auto backup: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Set backup frequency
     */
    fun setBackupFrequency(frequency: BackupFrequency) {
        viewModelScope.launch {
            try {
                preferencesManager.setBackupFrequency(frequency)
                _uiState.value = _uiState.value.copy(backupFrequency = frequency)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to set backup frequency: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Toggle encryption
     */
    fun toggleEncryption() {
        viewModelScope.launch {
            try {
                val newState = !_uiState.value.isEncryptionEnabled
                preferencesManager.setEncryptionEnabled(newState)
                _uiState.value = _uiState.value.copy(isEncryptionEnabled = newState)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to toggle encryption: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Toggle biometric authentication
     */
    fun toggleBiometric() {
        viewModelScope.launch {
            try {
                val newState = !_uiState.value.isBiometricEnabled
                preferencesManager.setBiometricEnabled(newState)
                _uiState.value = _uiState.value.copy(isBiometricEnabled = newState)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to toggle biometric: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

/**
 * UI state for Settings screen
 */
data class SettingsUiState(
    val currentTheme: ThemeMode = ThemeMode.SYSTEM,
    val isDashboardEnabled: Boolean = true,
    val isAnalyticsEnabled: Boolean = false,
    val isAutoBackupEnabled: Boolean = false,
    val backupFrequency: BackupFrequency = BackupFrequency.WEEKLY,
    val isEncryptionEnabled: Boolean = true,
    val isBiometricEnabled: Boolean = false,
    val error: String? = null
)

/**
 * Backup frequency options
 */
enum class BackupFrequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    NEVER
}
