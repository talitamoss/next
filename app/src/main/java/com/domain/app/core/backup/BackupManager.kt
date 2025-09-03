// app/src/main/java/com/domain/app/core/backup/BackupManager.kt
package com.domain.app.core.backup

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.domain.app.core.data.DataPoint
import com.domain.app.core.data.DataRepository
import com.domain.app.core.storage.encryption.EncryptionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.io.*
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages backup and restore operations for app data
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataRepository: DataRepository,
    private val encryptionManager: EncryptionManager
) {
    
    companion object {
        private const val BACKUP_VERSION = 1
        private const val BACKUP_FILE_PREFIX = "app_backup"
        private const val BACKUP_FILE_EXTENSION = ".bak"
        private const val ENCRYPTED_EXTENSION = ".enc"
        private const val BACKUP_DIR = "backups"
        private const val MAX_BACKUPS = 10
    }
    
    /**
     * Create a complete backup of all app data
     */
    suspend fun createBackup(
        location: Uri? = null,
        encrypt: Boolean = true
    ): BackupResult {
        return try {
            // Get all data from repository - using Flow
            val dataPoints = dataRepository.getAllDataPoints().first()
            
            // Create backup data structure
            val backupData = BackupData(
                version = BACKUP_VERSION,
                timestamp = System.currentTimeMillis(),
                dataPoints = dataPoints,
                metadata = createBackupMetadata()
            )
            
            // Serialize to JSON
            val jsonData = serializeBackupData(backupData)
            
            // Encrypt if requested
            val finalData = if (encrypt) {
                encryptBackupData(jsonData)
            } else {
                jsonData.toByteArray()
            }
            
            // Save to file
            val backupFile = if (location != null) {
                saveToExternalLocation(location, finalData, encrypt)
            } else {
                saveToInternalStorage(finalData, encrypt)
            }
            
            // Clean up old backups
            cleanupOldBackups()
            
            BackupResult.Success(
                backupFile = backupFile,
                itemCount = dataPoints.size,
                sizeBytes = finalData.size.toLong(),
                encrypted = encrypt
            )
        } catch (e: Exception) {
            BackupResult.Error(e.message ?: "Unknown error during backup")
        }
    }
    
    /**
     * Restore data from a backup file
     */
    suspend fun restoreBackup(
        backupUri: Uri,
        decrypt: Boolean = true
    ): RestoreResult {
        return try {
            // Read backup file
            val backupData = readBackupFile(backupUri, decrypt)
            
            // Validate backup version
            if (backupData.version > BACKUP_VERSION) {
                return RestoreResult.Error("Backup version not supported")
            }
            
            // Clear existing data (optional - could merge instead)
            dataRepository.clearAllData()
            
            // Restore data points
            var restoredCount = 0
            backupData.dataPoints.forEach { dataPoint ->
                dataRepository.insertDataPoint(dataPoint)
                restoredCount++
            }
            
            RestoreResult.Success(
                itemCount = restoredCount,
                timestamp = backupData.timestamp
            )
        } catch (e: Exception) {
            RestoreResult.Error(e.message ?: "Unknown error during restore")
        }
    }
    
    /**
     * List available backup files
     */
    fun listBackups(): List<BackupInfo> {
        val backupDir = File(context.filesDir, BACKUP_DIR)
        if (!backupDir.exists()) return emptyList()
        
        return backupDir.listFiles { file ->
            file.name.startsWith(BACKUP_FILE_PREFIX) && 
            (file.name.endsWith(BACKUP_FILE_EXTENSION) || 
             file.name.endsWith(ENCRYPTED_EXTENSION))
        }?.map { file ->
            BackupInfo(
                filename = file.name,
                timestamp = file.lastModified(),
                sizeBytes = file.length(),
                encrypted = file.name.endsWith(ENCRYPTED_EXTENSION),
                uri = Uri.fromFile(file)
            )
        }?.sortedByDescending { it.timestamp } ?: emptyList()
    }
    
    /**
     * Delete a specific backup
     */
    fun deleteBackup(backupUri: Uri): Boolean {
        return try {
            val file = File(backupUri.path ?: return false)
            file.delete()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get backup directory size
     */
    fun getBackupStorageSize(): Long {
        val backupDir = File(context.filesDir, BACKUP_DIR)
        if (!backupDir.exists()) return 0
        
        return backupDir.walkTopDown()
            .filter { it.isFile }
            .map { it.length() }
            .sum()
    }
    
    // ========== PRIVATE HELPER METHODS ==========
    
    private fun createBackupMetadata(): BackupMetadata {
        return BackupMetadata(
            deviceModel = android.os.Build.MODEL,
            androidVersion = android.os.Build.VERSION.SDK_INT,
            appVersion = getAppVersion(),
            createdBy = "App Backup System"
        )
    }
    
    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }
    
    private fun serializeBackupData(backupData: BackupData): String {
        // Simple JSON serialization
        // In production, use kotlinx.serialization or Gson
        val dataPointsJson = backupData.dataPoints.joinToString(",", "[", "]") { dataPoint ->
            """
            {
                "id": "${dataPoint.id}",
                "pluginId": "${dataPoint.pluginId}",
                "timestamp": ${dataPoint.timestamp.toEpochMilli()},
                "type": "${dataPoint.type}",
                "value": ${serializeValue(dataPoint.value)}
            }
            """.trimIndent()
        }
        
        return """
        {
            "version": ${backupData.version},
            "timestamp": ${backupData.timestamp},
            "dataPoints": $dataPointsJson,
            "metadata": {
                "deviceModel": "${backupData.metadata.deviceModel}",
                "androidVersion": ${backupData.metadata.androidVersion},
                "appVersion": "${backupData.metadata.appVersion}",
                "createdBy": "${backupData.metadata.createdBy}"
            }
        }
        """.trimIndent()
    }
    
    private fun serializeValue(value: Any): String {
        return when (value) {
            is String -> "\"$value\""
            is Number -> value.toString()
            is Boolean -> value.toString()
            is Map<*, *> -> {
                value.entries.joinToString(",", "{", "}") { (k, v) ->
                    "\"$k\": ${serializeValue(v ?: "null")}"
                }
            }
            is List<*> -> {
                value.joinToString(",", "[", "]") { 
                    serializeValue(it ?: "null")
                }
            }
            else -> "\"${value}\""
        }
    }
    
    private fun encryptBackupData(data: String): ByteArray {
        return encryptionManager.encrypt(data.toByteArray()).let { encryptedData ->
            // Combine IV and encrypted data
            encryptedData.iv + encryptedData.data
        }
    }
    
    private fun decryptBackupData(data: ByteArray): String {
        // Split IV and encrypted data
        val iv = data.sliceArray(0..11)
        val encryptedBytes = data.sliceArray(12 until data.size)
        
        val decrypted = encryptionManager.decrypt(
            com.domain.app.core.storage.encryption.EncryptedData(encryptedBytes, iv)
        )
        return String(decrypted)
    }
    
    private fun saveToInternalStorage(data: ByteArray, encrypted: Boolean): Uri {
        val backupDir = File(context.filesDir, BACKUP_DIR)
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }
        
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val extension = if (encrypted) ENCRYPTED_EXTENSION else BACKUP_FILE_EXTENSION
        val filename = "${BACKUP_FILE_PREFIX}_$timestamp$extension"
        
        val file = File(backupDir, filename)
        file.writeBytes(data)
        
        return Uri.fromFile(file)
    }
    
    private fun saveToExternalLocation(uri: Uri, data: ByteArray, encrypted: Boolean): Uri {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(data)
        }
        return uri
    }
    
    private suspend fun readBackupFile(uri: Uri, decrypt: Boolean): BackupData {
        val data = context.contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.readBytes()
        } ?: throw IOException("Cannot read backup file")
        
        val jsonData = if (decrypt && uri.toString().endsWith(ENCRYPTED_EXTENSION)) {
            decryptBackupData(data)
        } else {
            String(data)
        }
        
        return parseBackupData(jsonData)
    }
    
    private fun parseBackupData(json: String): BackupData {
        // Simple JSON parsing - in production use proper JSON library
        // This is a simplified version for the example
        
        // Extract version
        val versionMatch = """"version":\s*(\d+)""".toRegex().find(json)
        val version = versionMatch?.groupValues?.get(1)?.toInt() ?: 1
        
        // Extract timestamp
        val timestampMatch = """"timestamp":\s*(\d+)""".toRegex().find(json)
        val timestamp = timestampMatch?.groupValues?.get(1)?.toLong() ?: 0L
        
        // Parse data points (simplified)
        val dataPoints = mutableListOf<DataPoint>()
        val dataPointsMatch = """"dataPoints":\s*\[(.*?)\]""".toRegex(RegexOption.DOT_MATCHES_ALL).find(json)
        val dataPointsJson = dataPointsMatch?.groupValues?.get(1) ?: ""
        
        // Parse each data point
        val dataPointPattern = """\{[^}]*\}""".toRegex()
        dataPointPattern.findAll(dataPointsJson).forEach { match ->
            val dpJson = match.value
            val id = """"id":\s*"([^"]+)"""".toRegex().find(dpJson)?.groupValues?.get(1) ?: ""
            val pluginId = """"pluginId":\s*"([^"]+)"""".toRegex().find(dpJson)?.groupValues?.get(1) ?: ""
            val dpTimestamp = """"timestamp":\s*(\d+)""".toRegex().find(dpJson)?.groupValues?.get(1)?.toLong() ?: 0L
            val type = """"type":\s*"([^"]+)"""".toRegex().find(dpJson)?.groupValues?.get(1) ?: ""
            val valueMatch = """"value":\s*(.+?)(?:,|\})""".toRegex().find(dpJson)
            val value = parseJsonValue(valueMatch?.groupValues?.get(1) ?: "null")
            
            dataPoints.add(
                DataPoint(
                    id = id,
                    pluginId = pluginId,
                    timestamp = Instant.ofEpochMilli(dpTimestamp),
                    type = type,
                    value = value
                )
            )
        }
        
        // Parse metadata
        val deviceModel = """"deviceModel":\s*"([^"]+)"""".toRegex().find(json)?.groupValues?.get(1) ?: ""
        val androidVersion = """"androidVersion":\s*(\d+)""".toRegex().find(json)?.groupValues?.get(1)?.toInt() ?: 0
        val appVersion = """"appVersion":\s*"([^"]+)"""".toRegex().find(json)?.groupValues?.get(1) ?: ""
        val createdBy = """"createdBy":\s*"([^"]+)"""".toRegex().find(json)?.groupValues?.get(1) ?: ""
        
        return BackupData(
            version = version,
            timestamp = timestamp,
            dataPoints = dataPoints,
            metadata = BackupMetadata(
                deviceModel = deviceModel,
                androidVersion = androidVersion,
                appVersion = appVersion,
                createdBy = createdBy
            )
        )
    }
    
private fun parseJsonValue(jsonValue: String): Any {
    val trimmed = jsonValue.trim()
    return when {
        trimmed == "null" -> ""
        trimmed.startsWith("\"") && trimmed.endsWith("\"") -> 
            trimmed.substring(1, trimmed.length - 1)
        trimmed == "true" -> true
        trimmed == "false" -> false
        trimmed.toDoubleOrNull() != null -> trimmed.toDouble()
        trimmed.startsWith("{") -> parseJsonObject(trimmed)  // NO CAST!
        trimmed.startsWith("[") -> parseJsonArray(trimmed)
        else -> trimmed
    }
}    
    private fun parseJsonObject(json: String): Map<String, Any> {
        // Simplified JSON object parsing
        val map = mutableMapOf<String, Any>()
        val content = json.trim().removePrefix("{").removeSuffix("}")
        if (content.isEmpty()) return map
        
        // This is a very basic parser - in production use a proper JSON library
        val pairs = content.split(",")
        pairs.forEach { pair ->
            val parts = pair.split(":")
            if (parts.size == 2) {
                val key = parts[0].trim().removeSurrounding("\"")
                val value = parseJsonValue(parts[1].trim())
                map[key] = value
            }
        }
        return map
    }
    
    private fun parseJsonArray(json: String): List<Any> {
        // Simplified JSON array parsing
        val content = json.trim().removePrefix("[").removeSuffix("]")
        if (content.isEmpty()) return emptyList()
        
        return content.split(",").map { parseJsonValue(it.trim()) }
    }
    
  fun cleanupOldBackups() {  // Changed from private to public
        val backupDir = File(context.filesDir, BACKUP_DIR)
        if (!backupDir.exists()) return
        
        val backups = backupDir.listFiles { file ->
            file.name.startsWith(BACKUP_FILE_PREFIX)
        }?.sortedByDescending { it.lastModified() } ?: return
        
        // Keep only MAX_BACKUPS most recent files
        if (backups.size > MAX_BACKUPS) {
            backups.drop(MAX_BACKUPS).forEach { it.delete() }
        }
    }

}

// ========== DATA CLASSES ==========

data class BackupData(
    val version: Int,
    val timestamp: Long,
    val dataPoints: List<DataPoint>,
    val metadata: BackupMetadata
)

data class BackupMetadata(
    val deviceModel: String,
    val androidVersion: Int,
    val appVersion: String,
    val createdBy: String
)

data class BackupInfo(
    val filename: String,
    val timestamp: Long,
    val sizeBytes: Long,
    val encrypted: Boolean,
    val uri: Uri
)

sealed class BackupResult {
    data class Success(
        val backupFile: Uri,
        val itemCount: Int,
        val sizeBytes: Long,
        val encrypted: Boolean
    ) : BackupResult()
    
    data class Error(val message: String) : BackupResult()
}

sealed class RestoreResult {
    data class Success(
        val itemCount: Int,
        val timestamp: Long
    ) : RestoreResult()
    
    data class Error(val message: String) : RestoreResult()
}
