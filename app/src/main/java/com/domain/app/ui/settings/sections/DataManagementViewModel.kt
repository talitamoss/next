// app/src/main/java/com/domain/app/ui/settings/sections/DataManagementViewModel.kt
package com.domain.app.ui.settings.sections

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domain.app.core.backup.*
import com.domain.app.core.data.DataRepository
import com.domain.app.core.export.ExportManager
import com.domain.app.core.export.ExportResult
import com.domain.app.core.plugin.Plugin
import com.domain.app.core.plugin.PluginManager
import com.domain.app.core.preferences.UserPreferences
import com.domain.app.ui.data.ExportOptions
import com.domain.app.ui.data.TimeFrame
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class DataManagementUiState(
    val totalDataPoints: Int = 0,
    val storageUsedMB: String = "0 MB",
    val lastBackupTime: String = "Never",
    val autoBackupEnabled: Boolean = false,
    val backupTime: String = "2:00 AM",
    val backupLocation: String = "Local storage",
    val backupFrequency: String = "daily",
    val backupWifiOnly: Boolean = true,
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val isBackingUp: Boolean = false,
    val exportProgress: Float = 0f,
    val availableBackups: List<File> = emptyList(),
    val availablePlugins: List<Plugin> = emptyList(),
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class DataManagementViewModel @Inject constructor(
    private val dataRepository: DataRepository,
    private val userPreferences: UserPreferences,
    private val backupManager: BackupManager,
    private val exportManager: ExportManager,
    private val pluginManager: PluginManager,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DataManagementUiState())
    val uiState: StateFlow<DataManagementUiState> = _uiState.asStateFlow()
    
    init {
        loadDataStatistics()
        loadBackupSettings()
        loadAvailablePlugins()
    }
    
    private fun loadDataStatistics() {
        viewModelScope.launch {
            try {
                val dataCount = dataRepository.getDataCount()
                val storageSize = calculateStorageSize()
                val lastBackup = getLastBackupTime()
                
                _uiState.update {
                    it.copy(
                        totalDataPoints = dataCount,
                        storageUsedMB = formatStorageSize(storageSize),
                        lastBackupTime = lastBackup
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to load statistics: ${e.message}") }
            }
        }
    }
    
    private fun loadAvailablePlugins() {
        viewModelScope.launch {
            try {
                val plugins = pluginManager.getAllActivePlugins()
                _uiState.update { it.copy(availablePlugins = plugins) }
            } catch (e: Exception) {
                // If plugins can't be loaded, we'll just have an empty list
                _uiState.update { it.copy(availablePlugins = emptyList()) }
            }
        }
    }
    
    private fun loadBackupSettings() {
        viewModelScope.launch {
            userPreferences.autoBackupEnabled.collect { enabled ->
                _uiState.update { it.copy(autoBackupEnabled = enabled) }
            }
        }
        
        viewModelScope.launch {
            userPreferences.backupFrequency.collect { frequency ->
                _uiState.update { it.copy(backupFrequency = frequency) }
            }
        }
        
        viewModelScope.launch {
            userPreferences.backupWifiOnly.collect { wifiOnly ->
                _uiState.update { it.copy(backupWifiOnly = wifiOnly) }
            }
        }
    }
    
    /**
     * Export data with enhanced options
     * NEW: Uses ExportOptions for filtering
     */
    fun exportDataWithOptions(options: ExportOptions) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, message = "Preparing export...") }
            
            try {
                val (startDate, endDate) = options.getDateRange()
                
                val result = exportManager.exportFilteredData(
                    context = context,
                    format = options.format,
                    pluginIds = if (options.selectedPlugins.isEmpty()) null else options.selectedPlugins,
                    startDate = startDate,
                    endDate = endDate,
                    encrypt = options.encrypt
                )
                
                when (result) {
                    is ExportResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isExporting = false,
                                message = "Export successful: ${result.fileName}"
                            )
                        }
                    }
                    is ExportResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isExporting = false,
                                error = result.message
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        error = "Export failed: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Legacy export method (kept for compatibility if needed)
     */
    fun exportData(format: com.domain.app.core.plugin.ExportFormat, encrypt: Boolean) {
        val options = ExportOptions(
            format = format,
            timeFrame = TimeFrame.ALL,
            selectedPlugins = _uiState.value.availablePlugins.map { it.id }.toSet(),
            encrypt = encrypt
        )
        exportDataWithOptions(options)
    }
    
    fun importData(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true) }
            
            try {
                // TODO: Implement actual import logic
                _uiState.update { 
                    it.copy(
                        isImporting = false,
                        message = "Import functionality coming soon"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        error = "Import failed: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun toggleAutoBackup(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setAutoBackupEnabled(enabled)
            
            if (enabled) {
                scheduleAutoBackup()
            } else {
                cancelAutoBackup()
            }
        }
    }
    
    fun setBackupFrequency(frequency: String) {
        viewModelScope.launch {
            userPreferences.setBackupFrequency(frequency)
            if (_uiState.value.autoBackupEnabled) {
                scheduleAutoBackup()
            }
        }
    }
    
    fun setBackupWifiOnly(wifiOnly: Boolean) {
        viewModelScope.launch {
            userPreferences.setBackupWifiOnly(wifiOnly)
        }
    }
    
    fun backupNow() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBackingUp = true) }
            
            try {
                val result = backupManager.createBackup()
                
                if (result.isSuccess) {
                    _uiState.update {
                        it.copy(
                            isBackingUp = false,
                            message = "Backup completed successfully",
                            lastBackupTime = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                                .format(Date())
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isBackingUp = false,
                            error = result.exceptionOrNull()?.message ?: "Backup failed"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isBackingUp = false,
                        error = "Backup failed: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun restoreBackup(backupFile: File) {
        viewModelScope.launch {
            try {
                val result = backupManager.restoreBackup(backupFile)
                
                if (result.isSuccess) {
                    _uiState.update {
                        it.copy(message = "Backup restored successfully")
                    }
                    loadDataStatistics() // Reload statistics after restore
                } else {
                    _uiState.update {
                        it.copy(error = result.exceptionOrNull()?.message ?: "Restore failed")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Restore failed: ${e.message}")
                }
            }
        }
    }
    
    fun clearAllData() {
        viewModelScope.launch {
            try {
                // Clear all data from repository
                dataRepository.deleteAllData()
                
                // Clear preferences
                userPreferences.clearAllPreferences()
                
                // Reset UI state
                _uiState.value = DataManagementUiState(
                    message = "All data cleared successfully"
                )
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = "Failed to clear data: ${e.message}")
                }
            }
        }
    }
    
    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    private suspend fun calculateStorageSize(): Long {
        // TODO: Calculate actual storage size
        return 0L
    }
    
    private fun formatStorageSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${bytes / (1024 * 1024)} MB"
        }
    }
    
    private fun getLastBackupTime(): String {
        // TODO: Get actual last backup time
        return "Never"
    }
    
    private fun scheduleAutoBackup() {
        // Use extension function from BackupWorker
        context.scheduleAutoBackup(
            frequency = _uiState.value.backupFrequency,
            wifiOnly = _uiState.value.backupWifiOnly
        )
    }
    
    private fun cancelAutoBackup() {
        // Use extension function from BackupWorker
        context.cancelAutoBackup()
    }
}
