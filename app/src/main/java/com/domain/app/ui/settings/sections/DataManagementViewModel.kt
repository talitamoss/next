// app/src/main/java/com/domain/app/ui/settings/DataManagementViewModel.kt
package com.domain.app.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domain.app.core.data.DataRepository
import com.domain.app.core.preferences.UserPreferences
import com.domain.app.ui.settings.sections.ExportFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val exportProgress: Float = 0f,
    val message: String? = null
)

@HiltViewModel
class DataManagementViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataRepository: DataRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DataManagementUiState())
    val uiState: StateFlow<DataManagementUiState> = _uiState.asStateFlow()
    
    init {
        loadDataStatistics()
        observeBackupSettings()
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
        viewModelScope.launch {
            userPreferences.autoBackupEnabled.collect { enabled ->
                _uiState.update { it.copy(autoBackupEnabled = enabled) }
            }
        }
    }
    
    fun toggleAutoBackup() {
        viewModelScope.launch {
            val newState = !_uiState.value.autoBackupEnabled
            userPreferences.setAutoBackupEnabled(newState)
            
            if (newState) {
                // Schedule automatic backups
                scheduleAutoBackup()
            } else {
                // Cancel scheduled backups
                cancelAutoBackup()
            }
        }
    }
    
    fun exportData(format: ExportFormat, encrypt: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, exportProgress = 0f) }
            
            try {
                // Simulate export progress
                for (i in 1..10) {
                    _uiState.update { it.copy(exportProgress = i * 0.1f) }
                    kotlinx.coroutines.delay(100)
                }
                
                // Perform actual export
                when (format) {
                    ExportFormat.JSON -> exportAsJson(encrypt)
                    ExportFormat.CSV -> exportAsCsv(encrypt)
                    ExportFormat.ZIP -> exportAsZip(encrypt)
                }
                
                _uiState.update { 
                    it.copy(
                        isExporting = false,
                        exportProgress = 0f,
                        message = "Data exported successfully!"
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
                // Create backup
                createBackup()
                
                // Update last backup time
                val currentTime = System.currentTimeMillis()
                userPreferences.setLastBackupTime(currentTime)
                
                _uiState.update { 
                    it.copy(
                        lastBackupTime = formatBackupTime(currentTime),
                        message = "Backup created successfully!"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(message = "Backup failed: ${e.message}")
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
        // TODO: Implement JSON export
        val data = dataRepository.getAllDataPointsList()
        // Convert to JSON and save to file
        // If encrypt = true, encrypt the JSON before saving
    }
    
    private suspend fun exportAsCsv(encrypt: Boolean) {
        // TODO: Implement CSV export
        val data = dataRepository.getAllDataPointsList()
        // Convert to CSV format and save
    }
    
    private suspend fun exportAsZip(encrypt: Boolean) {
        // TODO: Implement ZIP export with all data and attachments
        val data = dataRepository.getAllDataPointsList()
        // Create ZIP archive with all data
    }
    
    private suspend fun createBackup() {
        // TODO: Implement backup creation
        // This would typically:
        // 1. Export all data to JSON
        // 2. Encrypt with user's key
        // 3. Save to designated backup location
        // 4. Optionally sync to cloud if enabled
    }
    
    private fun scheduleAutoBackup() {
        // TODO: Use WorkManager to schedule daily backups
        // This would schedule a PeriodicWorkRequest
    }
    
    private fun cancelAutoBackup() {
        // TODO: Cancel WorkManager scheduled backups
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

// Extension to add missing UserPreferences functions
suspend fun UserPreferences.setLastBackupTime(timestamp: Long) {
    // TODO: Implement in UserPreferences
}

val UserPreferences.lastBackupTime: Flow<Long>
    get() = flowOf(0L) // TODO: Implement in UserPreferences
