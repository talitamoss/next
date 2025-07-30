package com.domain.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domain.app.core.data.DataRepository
import com.domain.app.core.preferences.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * ViewModel for Settings screen
 * 
 * File location: app/src/main/java/com/domain/app/ui/settings/SettingsViewModel.kt
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val dataRepository: DataRepository
) : ViewModel() {
    
    // UI State
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    // Theme mode
    val themeMode = preferencesManager.themeMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "system"
        )
    
    // Backup frequency
    val backupFrequency = preferencesManager.backupFrequency
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "weekly"
        )
    
    // Last backup time
    val lastBackupTime = preferencesManager.lastBackupTime
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0L
        )
    
    init {
        loadStorageInfo()
    }
    
    /**
     * Load storage information
     */
    private fun loadStorageInfo() {
        viewModelScope.launch {
            try {
                // This is a simplified version - in reality you'd calculate actual storage
                val dataPointCount = 0 // Would sum all plugin data counts
                val estimatedSize = dataPointCount * 1024L // Rough estimate: 1KB per data point
                
                _uiState.update { state ->
                    state.copy(
                        storageUsed = estimatedSize,
                        dataPointCount = dataPointCount
                    )
                }
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(error = e.message)
                }
            }
        }
    }
    
    /**
     * Update theme mode
     */
    fun updateThemeMode(mode: String) {
        viewModelScope.launch {
            preferencesManager.setThemeMode(mode)
        }
    }
    
    /**
     * Update backup frequency
     */
    fun updateBackupFrequency(frequency: String) {
        viewModelScope.launch {
            preferencesManager.setBackupFrequency(frequency)
        }
    }
    
    /**
     * Export all data
     */
    fun exportData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, error = null) }
            
            try {
                // TODO: Implement actual export functionality
                // This would:
                // 1. Gather all data from database
                // 2. Create a JSON or CSV file
                // 3. Save to user-selected location
                
                _uiState.update { it.copy(
                    isExporting = false,
                    lastExportTime = System.currentTimeMillis()
                )}
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isExporting = false,
                    error = e.message
                )}
            }
        }
    }
    
    /**
     * Import data from file
     */
    fun importData(uri: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, error = null) }
            
            try {
                // TODO: Implement actual import functionality
                // This would:
                // 1. Read the file from URI
                // 2. Parse the data
                // 3. Validate and insert into database
                
                _uiState.update { it.copy(
                    isImporting = false,
                    lastImportTime = System.currentTimeMillis()
                )}
                
                // Reload storage info after import
                loadStorageInfo()
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isImporting = false,
                    error = e.message
                )}
            }
        }
    }
    
    /**
     * Clear all data
     */
    fun clearAllData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isClearingData = true, error = null) }
            
            try {
                // TODO: Implement actual data clearing
                // This would delete all data points from the database
                
                _uiState.update { it.copy(
                    isClearingData = false,
                    storageUsed = 0,
                    dataPointCount = 0
                )}
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isClearingData = false,
                    error = e.message
                )}
            }
        }
    }
    
    /**
     * Clean up old data (older than specified days)
     */
    fun cleanupOldData(daysToKeep: Int) {
        viewModelScope.launch {
            try {
                val cutoffDate = Instant.now().minus(daysToKeep.toLong(), ChronoUnit.DAYS)
                dataRepository.deleteOldData(cutoffDate)
                
                // Reload storage info
                loadStorageInfo()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
    
    /**
     * Toggle developer mode
     */
    fun toggleDeveloperMode() {
        _uiState.update { state ->
            state.copy(isDeveloperMode = !state.isDeveloperMode)
        }
    }
    
    /**
     * Clear error
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * UI state for settings screen
 */
data class SettingsUiState(
    val storageUsed: Long = 0,
    val dataPointCount: Int = 0,
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val isClearingData: Boolean = false,
    val lastExportTime: Long? = null,
    val lastImportTime: Long? = null,
    val isDeveloperMode: Boolean = false,
    val error: String? = null
)
