// app/src/main/java/com/domain/app/ui/settings/DataManagementViewModel.kt
package com.domain.app.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domain.app.core.backup.*
import com.domain.app.core.data.DataRepository
import com.domain.app.core.preferences.UserPreferences
import com.domain.app.ui.settings.sections.ExportFormat
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
        loadDataStatistics()
        observeBackupSettings()
        loadAvailableBackups()
    }
    
    private fun loadDataStatistics() {
        viewModelScope.launch {
            // Get total data points
            dataRepository.getAllDataPoints().collect { dataPoints ->
                _uiState.update { state ->
                    state.copy(
                        totalDataPoints = dataPoints.size,
                        storageUsedMB = calculateStorageUsed(dataPoints.size)
                    )
                }
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
        // Auto backup enabled state
        viewModelScope.launch {
            userPreferences.autoBackupEnabled.collect { enabled ->
                _uiState.update { it.copy(autoBackupEnabled = enabled) }
            }
        }
        
        // Backup frequency
        viewModelScope.launch {
            userPreferences.backupFrequency.collect { frequency ->
                _uiState.update { it.copy(backupFrequency = frequency) }
            }
        }
        
        // Backup WiFi only
        viewModelScope.launch {
            userPreferences.backupWifiOnly.collect { wifiOnly ->
                _uiState.update { it.copy(backupWifiOnly = wifiOnly) }
            }
        }
        
        // Backup location
        viewModelScope.launch {
            userPreferences.backupLocation.collect { location ->
                _uiState.update { it.copy(backupLocation = location) }
            }
        }
    }
    
    private fun loadAvailableBackups() {
        val backups = backupManager.getAvailableBackups()
        _uiState.update { it.copy(availableBackups = backups) }
    }
    
    fun toggleAutoBackup() {
        viewModelScope.launch {
            val newState = !_uiState.value.autoBackupEnabled
            userPreferences.setAutoBackupEnabled(newState)
            
            if (newState) {
                // Schedule automatic backups using WorkManager
                context.scheduleAutoBackup(
                    frequency = _uiState.value.backupFrequency,
                    wifiOnly = _uiState.value.backupWifiOnly
                )
                
                _uiState.update { 
                    it.copy(message = "Auto-backup enabled")
                }
            } else {
                // Cancel scheduled backups
                context.cancelAutoBackup()
                
                _uiState.update { 
                    it.copy(message = "Auto-backup disabled")
                }
            }
        }
    }
    
    fun setBackupFrequency(frequency: String) {
        viewModelScope.launch {
            userPreferences.setBackupFrequency(frequency)
            
            // Reschedule backups with new frequency if enabled
            if (_uiState.value.autoBackupEnabled) {
                context.scheduleAutoBackup(
                    frequency = frequency,
                    wifiOnly = _uiState.value.backupWifiOnly
                )
            }
        }
    }
    
    fun setBackupWifiOnly(wifiOnly: Boolean) {
        viewModelScope.launch {
            userPreferences.setBackupWifiOnly(wifiOnly)
            
            // Reschedule backups with new WiFi setting if enabled
            if (_uiState.value.autoBackupEnabled) {
                context.scheduleAutoBackup(
                    frequency = _uiState.value.backupFrequency,
                    wifiOnly = wifiOnly
                )
            }
        }
    }
    
    fun exportData(format: ExportFormat, encrypt: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, exportProgress = 0f) }
            
            try {
                // Convert UI ExportFormat to BackupFormat
                val backupFormat = when (format) {
                    ExportFormat.JSON -> BackupFormat.JSON
                    ExportFormat.CSV -> BackupFormat.CSV
                    ExportFormat.ZIP -> BackupFormat.ZIP
                }
                
                // Create export with progress updates
                val exportFile = backupManager.exportData(
                    format = backupFormat,
                    encrypt = encrypt
                )
                
                if (exportFile != null) {
                    _uiState.update { 
                        it.copy(
                            isExporting = false,
                            exportProgress = 0f,
                            message = "Data exported to: ${exportFile.name}"
                        )
                    }
                } else {
                    _uiState.update { 
                        it.copy(
                            isExporting = false,
                            exportProgress = 0f,
                            message = "Export failed"
                        )
                    }
                }
                
                // Reload available backups
                loadAvailableBackups()
                
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
    
    fun importData(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true) }
            
            try {
                val result = backupManager.restoreBackup(
                    backupUri = uri,
                    overwriteExisting = false
                )
                
                when (result) {
                    is RestoreResult.Success -> {
                        _uiState.update { 
                            it.copy(
                                isImporting = false,
                                message = "Imported ${result.itemsRestored} items successfully"
                            )
                        }
                    }
                    is RestoreResult.Failure -> {
                        _uiState.update { 
                            it.copy(
                                isImporting = false,
                                message = "Import failed: ${result.error}"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isImporting = false,
                        message = "Import failed: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun backupNow() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBackingUp = true) }
            
            try {
                // Create backup using BackupManager
                val result = backupManager.createBackup(
                    format = BackupFormat.JSON,
                    includePluginData = true,
                    encrypt = true
                )
                
                when (result) {
                    is BackupResult.Success -> {
                        // Update last backup time
                        userPreferences.setLastBackupTime(result.timestamp)
                        
                        _uiState.update { 
                            it.copy(
                                isBackingUp = false,
                                lastBackupTime = formatBackupTime(result.timestamp),
                                message = "Backup created successfully (${result.itemCount} items)"
                            )
                        }
                        
                        // Reload available backups
                        loadAvailableBackups()
                    }
                    is BackupResult.Failure -> {
                        _uiState.update { 
                            it.copy(
                                isBackingUp = false,
                                message = "Backup failed: ${result.error}"
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
                
                // Cancel any scheduled backups
                context.cancelAutoBackup()
                
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
    
    fun deleteBackup(backupFile: File) {
        viewModelScope.launch {
            try {
                if (backupFile.delete()) {
                    _uiState.update { 
                        it.copy(message = "Backup deleted")
                    }
                    loadAvailableBackups()
                } else {
                    _uiState.update { 
                        it.copy(message = "Failed to delete backup")
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(message = "Error deleting backup: ${e.message}")
                }
            }
        }
    }
    
    fun restoreFromBackup(backupFile: File) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true) }
            
            try {
                val uri = Uri.fromFile(backupFile)
                val result = backupManager.restoreBackup(
                    backupUri = uri,
                    overwriteExisting = false
                )
                
                when (result) {
                    is RestoreResult.Success -> {
                        _uiState.update { 
                            it.copy(
                                isImporting = false,
                                message = "Restored ${result.itemsRestored} items"
                            )
                        }
                    }
                    is RestoreResult.Failure -> {
                        _uiState.update { 
                            it.copy(
                                isImporting = false,
                                message = "Restore failed: ${result.error}"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isImporting = false,
                        message = "Restore failed: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
    
    private fun calculateStorageUsed(dataPoints: Int): String {
        // Rough estimate: assume each data point is ~1KB
        val sizeInKB = dataPoints
        return when {
            sizeInKB < 1024 -> "$sizeInKB KB"
            else -> String.format("%.1f MB", sizeInKB / 1024.0)
        }
    }
    
    private fun formatBackupTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < 60_000 -> "Just now"
            diff < 3600_000 -> "${diff / 60_000} minutes ago"
            diff < 86400_000 -> "${diff / 3600_000} hours ago"
            diff < 604800_000 -> "${diff / 86400_000} days ago"
            else -> {
                val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                formatter.format(Date(timestamp))
            }
        }
    }
}
