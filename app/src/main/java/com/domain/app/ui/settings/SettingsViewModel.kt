package com.domain.app.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domain.app.core.data.DataRepository
import com.domain.app.core.plugin.PluginManager
import com.domain.app.core.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

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
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            userPreferences.isDarkMode.collect { isDarkMode ->
                _uiState.value = _uiState.value.copy(isDarkMode = isDarkMode)
            }
        }
        
        viewModelScope.launch {
            userPreferences.userName.collect { userName ->
                _uiState.value = _uiState.value.copy(userName = userName)
            }
        }
        
        viewModelScope.launch {
            val plugins = pluginManager.getAllActivePlugins()
            _uiState.value = _uiState.value.copy(enabledPluginCount = plugins.size)
        }
    }
    
    fun toggleDarkMode() {
        viewModelScope.launch {
            userPreferences.setDarkMode(!_uiState.value.isDarkMode)
        }
    }
    
    fun exportData() {
        viewModelScope.launch {
            // Implement data export logic
        }
    }
    
    fun importData() {
        viewModelScope.launch {
            // Implement data import logic
        }
    }
    
    fun clearAllData() {
        viewModelScope.launch {
            dataRepository.clearAllData()
        }
    }
    
    fun enablePlugin(pluginId: String) {
        viewModelScope.launch {
            pluginManager.enablePlugin(pluginId)
        }
    }
    
    fun disablePlugin(pluginId: String) {
        viewModelScope.launch {
            pluginManager.disablePlugin(pluginId)
        }
    }
}

data class SettingsUiState(
    val userName: String? = null,
    val isDarkMode: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val enabledPluginCount: Int = 0,
    val appVersion: String = "1.0.0"
)
