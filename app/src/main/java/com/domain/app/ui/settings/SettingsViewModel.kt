// app/src/main/java/com/domain/app/ui/settings/SettingsViewModel.kt
package com.domain.app.ui.settings

import android.content.Context
import android.content.pm.PackageInfo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domain.app.core.data.DataRepository
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
    private val userPreferences: UserPreferences
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
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
        
        // Load user profile
        viewModelScope.launch {
            userPreferences.userName.collect { name ->
                _uiState.update { it.copy(userName = name) }
            }
        }
        
        // Load user email (if available)
        viewModelScope.launch {
            userPreferences.userEmail.collect { email ->
                _uiState.update { it.copy(userEmail = email) }
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
    
    fun updateUserEmail(email: String) {
        viewModelScope.launch {
            userPreferences.setUserEmail(email)
        }
    }
    
    fun exportData() {
        viewModelScope.launch {
            // Handled by DataManagementViewModel
        }
    }
    
    fun importData() {
        viewModelScope.launch {
            // Handled by DataManagementViewModel
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

// UserPreferences extensions (to be added to UserPreferences.kt)
val UserPreferences.userEmail: Flow<String?>
    get() = flowOf(null) // TODO: Implement in UserPreferences

suspend fun UserPreferences.setUserEmail(email: String) {
    // TODO: Implement in UserPreferences
}

suspend fun UserPreferences.setAutoLockTimeout(minutes: Int) {
    // TODO: Implement in UserPreferences
}
