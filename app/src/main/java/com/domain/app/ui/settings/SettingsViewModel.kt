package com.domain.app.ui.settings

import android.content.Context
import android.content.pm.PackageInfo
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domain.app.core.data.DataRepository
import com.domain.app.core.data.export.ExportManager
import com.domain.app.core.plugin.PluginManager
import com.domain.app.core.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    // Profile
    val userName: String? = null,
    val userEmail: String? = null,
    
    // Aesthetic
    val isDarkMode: Boolean = false,
    val themeMode: String = "System",
    
    // Security
    val biometricEnabled: Boolean = false,
    val autoLockTimeout: Int = 5,
    
    // Plugins
    val enabledPluginCount: Int = 0,
    
    // Data Management
    val autoBackupEnabled: Boolean = false,
    val backupFrequency: String = "Daily",
    val lastExportTime: Long? = null,
    val exportInProgress: Boolean = false,
    
    // Notifications
    val notificationsEnabled: Boolean = true,
    
    // App Info
    val appVersion: String = "2.0.0",
    val buildNumber: String = "2024.1"
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataRepository: DataRepository,
    private val pluginManager: PluginManager,
    private val userPreferences: UserPreferences,
    private val exportManager: ExportManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    // Dialog states
    val showBackupFrequencyDialog = mutableStateOf(false)
    val showExportConfirmDialog = mutableStateOf(false)
    
    init {
        loadSettings()
        loadAppInfo()
    }
    
    private fun loadSettings() {
        // Load dark mode preference
        viewModelScope.launch {
            userPreferences.isDarkMode.collect { isDarkMode ->
                _uiState.update { it.copy(isDarkMode = isDarkMode) }
            }
        }
        
        // Load theme mode
        viewModelScope.launch {
            userPreferences.themeMode.collect { mode ->
                _uiState.update { 
                    it.copy(
                        themeMode = when(mode) {
                            "light" -> "Light"
                            "dark" -> "Dark"
                            else -> "System"
                        }
                    )
                }
            }
        }
        
        // Load auto backup settings
        viewModelScope.launch {
            userPreferences.autoBackupEnabled.collect { enabled ->
                _uiState.update { it.copy(autoBackupEnabled = enabled) }
            }
        }
        
        // Load backup frequency
        viewModelScope.launch {
            userPreferences.backupFrequency.collect { frequency ->
                _uiState.update { it.copy(backupFrequency = frequency) }
            }
        }
        
        // Load last backup time
        viewModelScope.launch {
            userPreferences.lastBackupTime.collect { timestamp ->
                if (timestamp > 0) {
                    _uiState.update { it.copy(lastExportTime = timestamp) }
                }
            }
        }
        
        // Load user profile
        viewModelScope.launch {
            userPreferences.userName.collect { name ->
                _uiState.update { it.copy(userName = name) }
            }
        }
        
        // Load biometric setting
        viewModelScope.launch {
            userPreferences.biometricAuthEnabled.collect { enabled ->
                _uiState.update { it.copy(biometricEnabled = enabled) }
            }
        }
        
        // Load notification setting
        viewModelScope.launch {
            userPreferences.notificationsEnabled.collect { enabled ->
                _uiState.update { it.copy(notificationsEnabled = enabled) }
            }
        }
        
        // Load plugin count
        viewModelScope.launch {
            val plugins = pluginManager.getAllActivePlugins()
            _uiState.update { it.copy(enabledPluginCount = plugins.size) }
        }
    }
    
    private fun loadAppInfo() {
        try {
            val packageInfo: PackageInfo = context.packageManager
                .getPackageInfo(context.packageName, 0)
            
            _uiState.update { 
                it.copy(
                    appVersion = packageInfo.versionName ?: "2.0.0",
                    buildNumber = packageInfo.versionCode.toString()
                )
            }
        } catch (e: Exception) {
            // Use default values if package info cannot be retrieved
        }
    }
    
    fun toggleDarkMode() {
        viewModelScope.launch {
            val newState = !_uiState.value.isDarkMode
            userPreferences.setDarkMode(newState)
            
            // Update theme mode to match
            userPreferences.setThemeMode(if (newState) "dark" else "light")
        }
    }
    
    fun toggleAutoBackup() {
        viewModelScope.launch {
            val newState = !_uiState.value.autoBackupEnabled
            userPreferences.setAutoBackupEnabled(newState)
        }
    }
    
    fun showBackupFrequencyDialog() {
        showBackupFrequencyDialog.value = true
    }
    
    fun hideBackupFrequencyDialog() {
        showBackupFrequencyDialog.value = false
    }
    
    fun setBackupFrequency(frequency: String) {
        viewModelScope.launch {
            userPreferences.setBackupFrequency(frequency)
            hideBackupFrequencyDialog()
        }
    }
    
    fun exportData() {
        if (_uiState.value.exportInProgress) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(exportInProgress = true) }
            
            try {
                val result = exportManager.exportAllData()
                
                if (result.isSuccess) {
                    val timestamp = System.currentTimeMillis()
                    userPreferences.setLastBackupTime(timestamp)
                    
                    _uiState.update { 
                        it.copy(
                            exportInProgress = false,
                            lastExportTime = timestamp
                        )
                    }
                } else {
                    _uiState.update { it.copy(exportInProgress = false) }
                    // Handle error - show toast or snackbar
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(exportInProgress = false) }
                // Handle exception - show error message
            }
        }
    }
    
    fun toggleBiometric() {
        viewModelScope.launch {
            val newState = !_uiState.value.biometricEnabled
            userPreferences.setBiometricAuthEnabled(newState)
        }
    }
    
    fun setAutoLockTimeout(minutes: Int) {
        viewModelScope.launch {
            userPreferences.setAutoLockTimeout(minutes)
            _uiState.update { it.copy(autoLockTimeout = minutes) }
        }
    }
    
    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            userPreferences.setThemeMode(mode.lowercase())
            
            // Update dark mode based on theme selection
            when (mode.lowercase()) {
                "dark" -> userPreferences.setDarkMode(true)
                "light" -> userPreferences.setDarkMode(false)
                // "system" - let system decide
            }
        }
    }
    
    fun updateUserName(name: String) {
        viewModelScope.launch {
            userPreferences.setUserName(name)
        }
    }
    
    fun clearAllData() {
        viewModelScope.launch {
            dataRepository.clearAllData()
            userPreferences.clearAllPreferences()
        }
    }
    
    fun enablePlugin(pluginId: String) {
        viewModelScope.launch {
            pluginManager.enablePlugin(pluginId)
            refreshPluginCount()
        }
    }
    
    fun disablePlugin(pluginId: String) {
        viewModelScope.launch {
            pluginManager.disablePlugin(pluginId)
            refreshPluginCount()
        }
    }
    
    private fun refreshPluginCount() {
        viewModelScope.launch {
            val plugins = pluginManager.getAllActivePlugins()
            _uiState.update { it.copy(enabledPluginCount = plugins.size) }
        }
    }
}
