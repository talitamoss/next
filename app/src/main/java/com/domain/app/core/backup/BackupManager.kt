// app/src/main/java/com/domain/app/core/backup/BackupManager.kt
package com.domain.app.core.backup

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.domain.app.core.data.DataPoint
import com.domain.app.core.data.DataRepository
import com.domain.app.core.encryption.EncryptionManager
import com.domain.app.core.plugin.PluginManager
import com.domain.app.core.preferences.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result of a backup operation
 */
sealed class BackupResult {
    data class Success(
        val backupFile: File,
        val itemCount: Int,
        val sizeBytes: Long,
        val timestamp: Long = System.currentTimeMillis()
    ) : BackupResult()
    
    data class Failure(val error: String) : BackupResult()
}

/**
 * Result of a restore operation
 */
sealed class RestoreResult {
    data class Success(
        val itemsRestored: Int,
        val timestamp: Long = System.currentTimeMillis()
    ) : RestoreResult()
    
    data class Failure(val error: String) : RestoreResult()
}

/**
 * Backup file format
 */
enum class BackupFormat {
    JSON,
    CSV,
    ZIP
}

/**
 * Data structure for backup metadata
 */
@Serializable
data class BackupMetadata(
    val version: Int = 1,
    val appVersion: String,
    val timestamp: Long,
    val deviceName: String,
    val dataPointCount: Int,
    val pluginDataIncluded: Boolean,
    val encrypted: Boolean
)

/**
 * Complete backup data structure
 */
@Serializable
data class BackupData(
    val metadata: BackupMetadata,
    val dataPoints: List<SerializableDataPoint>,
    val preferences: Map<String, String>,
    val pluginData: Map<String, String>? = null
)

/**
 * Serializable version of DataPoint
 */
@Serializable
data class SerializableDataPoint(
    val id: String,
    val pluginId: String,
    val type: String,
    val data: Map<String, String>,
    val timestamp: Long,
    val synced: Boolean = false,
    val encrypted: Boolean = false
)

/**
 * Manages backup and restore operations for app data
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataRepository: DataRepository,
    private val encryptionManager: EncryptionManager,
    private val pluginManager: PluginManager,
    private val userPreferences: UserPreferences
) {
    
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    private val backupDir: File by lazy {
        File(context.filesDir, "backups").apply {
            if (!exists()) mkdirs()
        }
    }
    
    private val exportDir: File by lazy {
        File(context.getExternalFilesDir(null), "exports").apply {
            if (!exists()) mkdirs()
        }
    }
    
    /**
     * Create a backup of all app data
     */
    suspend fun createBackup(
        format: BackupFormat = BackupFormat.JSON,
        includePluginData: Boolean = true,
        encrypt: Boolean = true
    ): BackupResult = withContext(Dispatchers.IO) {
        try {
            // Collect all data
            val dataPoints = dataRepository.getAllDataPointsList()
            val preferences = userPreferences.exportPreferences()
            
            // Collect plugin data if requested
            val pluginData = if (includePluginData) {
                collectPluginData()
            } else null
            
            // Create backup data structure
            val backupData = BackupData(
                metadata = BackupMetadata(
                    appVersion = getAppVersion(),
                    timestamp = System.currentTimeMillis(),
                    deviceName = android.os.Build.MODEL,
                    dataPointCount = dataPoints.size,
                    pluginDataIncluded = includePluginData,
                    encrypted = encrypt
                ),
                dataPoints = dataPoints.map { it.toSerializable() },
                preferences = preferences.mapValues { it.value.toString() },
                pluginData = pluginData
            )
            
            // Create backup file based on format
            val backupFile = when (format) {
                BackupFormat.JSON -> createJsonBackup(backupData, encrypt)
                BackupFormat.CSV -> createCsvBackup(dataPoints, encrypt)
                BackupFormat.ZIP -> createZipBackup(backupData, encrypt)
            }
            
            // Update last backup time
            userPreferences.setLastBackupTime(System.currentTimeMillis())
            
            BackupResult.Success(
                backupFile = backupFile,
                itemCount = dataPoints.size,
                sizeBytes = backupFile.length()
            )
        } catch (e: Exception) {
            BackupResult.Failure("Backup failed: ${e.message}")
        }
    }
    
    /**
     * Create a JSON backup file
     */
    private suspend fun createJsonBackup(
        backupData: BackupData,
        encrypt: Boolean
    ): File {
        val fileName = generateBackupFileName("json")
        val backupFile = File(backupDir, fileName)
        
        val jsonContent = json.encodeToString(backupData)
        
        if (encrypt) {
            val encryptedContent = encryptionManager.encryptString(jsonContent)
            backupFile.writeText(encryptedContent)
        } else {
            backupFile.writeText(jsonContent)
        }
        
        return backupFile
    }
    
    /**
     * Create a CSV backup file
     */
    private suspend fun createCsvBackup(
        dataPoints: List<DataPoint>,
        encrypt: Boolean
    ): File {
        val fileName = generateBackupFileName("csv")
        val backupFile = File(exportDir, fileName)
        
        val csvContent = buildString {
            // Header
            appendLine("ID,Plugin,Type,Data,Timestamp,Synced")
            
            // Data rows
            dataPoints.forEach { point ->
                append("\"${point.id}\",")
                append("\"${point.pluginId}\",")
                append("\"${point.type}\",")
                append("\"${point.data}\",")
                append("${point.timestamp},")
                appendLine(point.synced)
            }
        }
        
        if (encrypt) {
            val encryptedContent = encryptionManager.encryptString(csvContent)
            backupFile.writeText(encryptedContent)
        } else {
            backupFile.writeText(csvContent)
        }
        
        return backupFile
    }
    
    /**
     * Create a ZIP backup file containing all data
     */
    private suspend fun createZipBackup(
        backupData: BackupData,
        encrypt: Boolean
    ): File {
        val fileName = generateBackupFileName("zip")
        val backupFile = File(exportDir, fileName)
        
        ZipOutputStream(FileOutputStream(backupFile)).use { zipOut ->
            // Add metadata
            val metadataEntry = ZipEntry("metadata.json")
            zipOut.putNextEntry(metadataEntry)
            val metadataJson = json.encodeToString(backupData.metadata)
            zipOut.write(
                if (encrypt) {
                    encryptionManager.encryptString(metadataJson).toByteArray()
                } else {
                    metadataJson.toByteArray()
                }
            )
            zipOut.closeEntry()
            
            // Add data points
            val dataEntry = ZipEntry("data.json")
            zipOut.putNextEntry(dataEntry)
            val dataJson = json.encodeToString(backupData.dataPoints)
            zipOut.write(
                if (encrypt) {
                    encryptionManager.encryptString(dataJson).toByteArray()
                } else {
                    dataJson.toByteArray()
                }
            )
            zipOut.closeEntry()
            
            // Add preferences
            val prefsEntry = ZipEntry("preferences.json")
            zipOut.putNextEntry(prefsEntry)
            val prefsJson = json.encodeToString(backupData.preferences)
            zipOut.write(
                if (encrypt) {
                    encryptionManager.encryptString(prefsJson).toByteArray()
                } else {
                    prefsJson.toByteArray()
                }
            )
            zipOut.closeEntry()
            
            // Add plugin data if present
            backupData.pluginData?.let { pluginData ->
                val pluginEntry = ZipEntry("plugins.json")
                zipOut.putNextEntry(pluginEntry)
                val pluginJson = json.encodeToString(pluginData)
                zipOut.write(
                    if (encrypt) {
                        encryptionManager.encryptString(pluginJson).toByteArray()
                    } else {
                        pluginJson.toByteArray()
                    }
                )
                zipOut.closeEntry()
            }
        }
        
        return backupFile
    }
    
    /**
     * Restore data from a backup file
     */
    suspend fun restoreBackup(
        backupUri: Uri,
        overwriteExisting: Boolean = false
    ): RestoreResult = withContext(Dispatchers.IO) {
        try {
            // Read backup file
            val inputStream = context.contentResolver.openInputStream(backupUri)
                ?: return@withContext RestoreResult.Failure("Cannot open backup file")
            
            val content = inputStream.bufferedReader().use { it.readText() }
            
            // Try to decrypt if needed
            val jsonContent = try {
                encryptionManager.decryptString(content)
            } catch (e: Exception) {
                // Not encrypted or decryption failed, try as plain text
                content
            }
            
            // Parse backup data
            val backupData = json.decodeFromString<BackupData>(jsonContent)
            
            // Validate backup version
            if (backupData.metadata.version > 1) {
                return@withContext RestoreResult.Failure("Backup version not supported")
            }
            
            // Clear existing data if requested
            if (overwriteExisting) {
                dataRepository.clearAllData()
            }
            
            // Restore data points
            var restoredCount = 0
            backupData.dataPoints.forEach { serializedPoint ->
                val dataPoint = serializedPoint.toDataPoint()
                dataRepository.insertDataPoint(dataPoint)
                restoredCount++
            }
            
            // Restore preferences
            // Note: This would need implementation in UserPreferences to import preferences
            
            // Restore plugin data if present
            backupData.pluginData?.let { pluginData ->
                restorePluginData(pluginData)
            }
            
            RestoreResult.Success(itemsRestored = restoredCount)
        } catch (e: Exception) {
            RestoreResult.Failure("Restore failed: ${e.message}")
        }
    }
    
    /**
     * Export data to external storage
     */
    suspend fun exportData(
        format: BackupFormat,
        encrypt: Boolean = false
    ): File? = withContext(Dispatchers.IO) {
        val result = createBackup(format, includePluginData = true, encrypt = encrypt)
        when (result) {
            is BackupResult.Success -> {
                // Copy to exports directory for user access
                val exportFile = File(exportDir, result.backupFile.name)
                result.backupFile.copyTo(exportFile, overwrite = true)
                exportFile
            }
            is BackupResult.Failure -> null
        }
    }
    
    /**
     * Get list of available backups
     */
    fun getAvailableBackups(): List<File> {
        return backupDir.listFiles { file ->
            file.extension in listOf("json", "csv", "zip")
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
    
    /**
     * Delete old backups to save space
     */
    suspend fun cleanupOldBackups(keepCount: Int = 5) = withContext(Dispatchers.IO) {
        val backups = getAvailableBackups()
        if (backups.size > keepCount) {
            backups.drop(keepCount).forEach { file ->
                file.delete()
            }
        }
    }
    
    /**
     * Generate a backup file name with timestamp
     */
    private fun generateBackupFileName(extension: String): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val timestamp = dateFormat.format(Date())
        return "backup_${timestamp}.$extension"
    }
    
    /**
     * Get app version for backup metadata
     */
    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }
    
    /**
     * Collect plugin-specific data for backup
     */
    private suspend fun collectPluginData(): Map<String, String> {
        val pluginData = mutableMapOf<String, String>()
        
        // Get all active plugins
        val activePlugins = pluginManager.getAllActivePlugins()
        
        activePlugins.forEach { plugin ->
            // Each plugin could implement a backup interface
            // For now, we'll just store plugin states
            pluginData[plugin.id] = "active"
        }
        
        return pluginData
    }
    
    /**
     * Restore plugin-specific data from backup
     */
    private suspend fun restorePluginData(pluginData: Map<String, String>) {
        pluginData.forEach { (pluginId, state) ->
            if (state == "active") {
                // Re-enable plugin if it was active in backup
                pluginManager.enablePlugin(pluginId)
            }
        }
    }
    
    /**
     * Convert DataPoint to SerializableDataPoint
     */
    private fun DataPoint.toSerializable(): SerializableDataPoint {
        return SerializableDataPoint(
            id = this.id,
            pluginId = this.pluginId,
            type = this.type,
            data = this.data,
            timestamp = this.timestamp,
            synced = this.synced,
            encrypted = this.encrypted
        )
    }
    
    /**
     * Convert SerializableDataPoint back to DataPoint
     */
    private fun SerializableDataPoint.toDataPoint(): DataPoint {
        return DataPoint(
            id = this.id,
            pluginId = this.pluginId,
            type = this.type,
            data = this.data,
            timestamp = this.timestamp,
            synced = this.synced,
            encrypted = this.encrypted
        )
    }
}
