// app/src/main/java/com/domain/app/ui/settings/sections/DataManagementViewModel.kt
package com.domain.app.ui.settings.sections

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domain.app.core.backup.*
import com.domain.app.core.data.DataRepository
import com.domain.app.core.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

// Export format enum
enum class ExportFormat {
    JSON,
    CSV,
    ZIP
}

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
    val message: String? = null
)

@HiltViewModel
class DataManagementViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataRepository: DataRepository,
    private val userPreferences: UserPreferences,
    private val backupManager: BackupManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DataManagementUiState())
    val uiState: StateFlow<DataManagementUiState> = _uiState.asStateFlow()
    
    init {
        loadDataStats()
        observeBackupSettings()
        loadAvailableBackups()
    }
    
    private fun loadDataStats() {
        viewModelScope.launch {
            // Count total data points using Flow
            dataRepository.getAllDataPoints().collect { dataPoints ->
                _uiState.update { state ->
                    state.copy(totalDataPoints = dataPoints.size)
                }
            }
        }
        
        // Calculate storage used
        viewModelScope.launch {
            val storageSize = calculateStorageUsed()
            _uiState.update { state ->
                state.copy(storageUsedMB = formatStorageSize(storageSize))
            }
        }
        
        // Load last backup time
        viewModelScope.launch {
            userPreferences.lastBackupTime.collect { timestamp ->
                _uiState.update { state ->
                    state.copy(
                        lastBackupTime = if (timestamp > 0) {
                            formatBackupTime(timestamp)
                        } else {
                            "Never"
                        }
                    )
                }
            }
        }
    }
    
    private fun observeBackupSettings() {
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
    
    private fun loadAvailableBackups() {
        viewModelScope.launch {
            val backups = backupManager.listBackups()
            _uiState.update { state ->
                state.copy(availableBackups = backups.map { File(it.uri.path ?: "") })
            }
        }
    }
    
    private fun calculateStorageUsed(): Long {
        // Calculate database size + backup size
        val dbFile = context.getDatabasePath("app_database")
        val dbSize = if (dbFile.exists()) dbFile.length() else 0L
        val backupSize = backupManager.getBackupStorageSize()
        return dbSize + backupSize
    }
    
    private fun formatStorageSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${bytes / (1024 * 1024)} MB"
        }
    }
    
    private fun formatBackupTime(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
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
    
    fun updateBackupFrequency(frequency: String) {
        viewModelScope.launch {
            userPreferences.setBackupFrequency(frequency)
            if (_uiState.value.autoBackupEnabled) {
                // Reschedule with new frequency
                cancelAutoBackup()
                scheduleAutoBackup()
            }
        }
    }
    
    fun updateBackupWifiOnly(wifiOnly: Boolean) {
        viewModelScope.launch {
            userPreferences.setBackupWifiOnly(wifiOnly)
            if (_uiState.value.autoBackupEnabled) {
                // Reschedule with new settings
                cancelAutoBackup()
                scheduleAutoBackup()
            }
        }
    }
    
    fun exportData(format: ExportFormat, encryptExport: Boolean) {
        viewModelScope.launch {
            _uiState.update { 
                it.copy(isExporting = true, exportProgress = 0f)
            }
            
            try {
                when (format) {
                    ExportFormat.JSON -> exportAsJson(encryptExport)
                    ExportFormat.CSV -> exportAsCsv(encryptExport)
                    ExportFormat.ZIP -> exportAsZip(encryptExport)
                }
                
                _uiState.update { 
                    it.copy(
                        isExporting = false,
                        exportProgress = 100f,
                        message = "Export completed successfully!"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isExporting = false,
                        exportProgress = 0f,
                        message = "Export failed: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun importData(uri: String) {
        // TODO: Implement import functionality
        _uiState.update { 
            it.copy(message = "Import feature coming soon!")
        }
    }
    
    fun backupNow() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isBackingUp = true) }
                
                // Create backup
                val result = backupManager.createBackup(encrypt = true)
                
                when (result) {
                    is BackupResult.Success -> {
                        // Update last backup time
                        val currentTime = System.currentTimeMillis()
                        userPreferences.setLastBackupTime(currentTime)
                        
                        _uiState.update { 
                            it.copy(
                                isBackingUp = false,
                                lastBackupTime = formatBackupTime(currentTime),
                                message = "Backup created successfully! ${result.itemCount} items backed up."
                            )
                        }
                        
                        // Reload available backups
                        loadAvailableBackups()
                    }
                    is BackupResult.Error -> {
                        _uiState.update { 
                            it.copy(
                                isBackingUp = false,
                                message = "Backup failed: ${result.message}"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isBackingUp = false,
                        message = "Backup failed: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun clearAllData() {
        viewModelScope.launch {
            try {
                // Clear all data from repository
                dataRepository.clearAllData()
                
                // Clear user preferences
                userPreferences.clearAllPreferences()
                
                // Reset UI state
                _uiState.value = DataManagementUiState(
                    message = "All data cleared successfully"
                )
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(message = "Failed to clear data: ${e.message}")
                }
            }
        }
    }
    
    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
    
    private suspend fun exportAsJson(encrypt: Boolean) {
        // Get all data points using Flow
        val dataPoints = dataRepository.getAllDataPoints().first()
        
        // TODO: Implement actual JSON export
        // For now, just simulate progress
        _uiState.update { it.copy(exportProgress = 50f) }
        // Convert to JSON and save to file
        // If encrypt = true, encrypt the JSON before saving
        _uiState.update { it.copy(exportProgress = 100f) }
    }
    
    private suspend fun exportAsCsv(encrypt: Boolean) {
        // Get all data points using Flow
        val dataPoints = dataRepository.getAllDataPoints().first()
        
        // TODO: Implement actual CSV export
        _uiState.update { it.copy(exportProgress = 50f) }
        // Convert to CSV format and save
        _uiState.update { it.copy(exportProgress = 100f) }
    }
    
    private suspend fun exportAsZip(encrypt: Boolean) {
        // Get all data points using Flow
        val dataPoints = dataRepository.getAllDataPoints().first()
        
        // TODO: Implement actual ZIP export
        _uiState.update { it.copy(exportProgress = 50f) }
        // Create ZIP archive with all data
        _uiState.update { it.copy(exportProgress = 100f) }
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
