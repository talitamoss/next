// app/src/main/java/com/domain/app/core/export/ExportManager.kt
package com.domain.app.core.export

import android.annotation.TargetApi
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.domain.app.core.data.DataPoint
import com.domain.app.core.data.DataRepository
import com.domain.app.core.plugin.Plugin
import com.domain.app.core.plugin.PluginManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages data export functionality for user data sovereignty.
 * Supports multiple export formats with plugin-specific formatting.
 */
@Singleton
class ExportManager @Inject constructor(
    private val dataRepository: DataRepository,
    private val pluginManager: PluginManager
) {
    
    companion object {
        private const val TAG = "ExportManager"
    }
    
    /**
     * Export all user data to CSV format in user-accessible Downloads folder
     * VERIFIED: Uses Flow.first() correctly
     */
    suspend fun exportAllDataToCsv(context: Context): ExportResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "=== EXPORT TO USER DOWNLOADS START ===")
        
        try {
            // Step 1: Get data
            val plugins = pluginManager.getAllActivePlugins()
            Log.d(TAG, "Found ${plugins.size} active plugins")
            
            // Collect the Flow to get List<DataPoint>
            // CORRECT: getRecentData() returns Flow<List<DataPoint>>, .first() gets the List
            val allData = dataRepository.getRecentData(24 * 365).first()
            Log.d(TAG, "Retrieved ${allData.size} total data points")
            
            if (allData.isEmpty()) {
                return@withContext ExportResult.Error("No data to export")
            }
            
            // Step 2: Generate content
            val pluginMap = plugins.associateBy { it.id }
            val csvContent = generateCsvContent(allData, pluginMap)
            Log.d(TAG, "Generated CSV content: ${csvContent.length} characters")
            
            // Step 3: Save to user Downloads
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
            val fileName = "behavioral_data_export_$timestamp.csv"
            
            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Log.d(TAG, "Using MediaStore for Downloads folder")
                saveToDownloadsMediaStore(context, fileName, csvContent, allData.size)
            } else {
                Log.d(TAG, "Using legacy Downloads approach")
                saveToDownloadsLegacy(context, fileName, csvContent, allData.size)
            }
            
            Log.d(TAG, "=== EXPORT TO USER DOWNLOADS COMPLETED ===")
            return@withContext result
            
        } catch (e: Exception) {
            Log.e(TAG, "Export to Downloads failed", e)
            ExportResult.Error("Export failed: ${e.message}")
        }
    }
    
    /**
     * Export data for a specific plugin
     * FIXED: Handles Flow vs suspend function correctly
     */
    suspend fun exportPluginData(
        context: Context,
        pluginId: String,
        startDate: Instant? = null,
        endDate: Instant? = null
    ): ExportResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "=== PLUGIN EXPORT: $pluginId ===")
        
        try {
            val plugin = pluginManager.getPlugin(pluginId)
            if (plugin == null) {
                return@withContext ExportResult.Error("Plugin not found: $pluginId")
            }
            
            // FIXED: Handle different return types correctly
            val data: List<DataPoint> = if (startDate != null && endDate != null) {
                // getPluginDataInRange returns List<DataPoint> directly (suspend function)
                // NO .first() needed!
                dataRepository.getPluginDataInRange(pluginId, startDate, endDate)
            } else {
                // getPluginData returns Flow<List<DataPoint>>
                // .first() IS needed to get the List from the Flow
                dataRepository.getPluginData(pluginId).first()
            }
            
            if (data.isEmpty()) {
                return@withContext ExportResult.Error("No data found for ${plugin.metadata.name}")
            }
            
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
            val fileName = "${plugin.metadata.name.lowercase().replace(" ", "_")}_export_$timestamp.csv"
            val csvContent = generatePluginCsvContent(data, plugin)
            
            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveToDownloadsMediaStore(context, fileName, csvContent, data.size)
            } else {
                saveToDownloadsLegacy(context, fileName, csvContent, data.size)
            }
            
            Log.d(TAG, "Plugin export completed")
            return@withContext result
            
        } catch (e: Exception) {
            Log.e(TAG, "Plugin export failed", e)
            ExportResult.Error("Export failed: ${e.message}")
        }
    }
    
    /**
     * Save to Downloads using MediaStore (Android 10+)
     */
    @TargetApi(Build.VERSION_CODES.Q)
    private fun saveToDownloadsMediaStore(
        context: Context, 
        fileName: String, 
        content: String, 
        recordCount: Int
    ): ExportResult {
        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            
            Log.d(TAG, "Creating file in Downloads via MediaStore...")
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            
            if (uri == null) {
                throw Exception("Failed to create file in Downloads")
            }
            
            Log.d(TAG, "Writing to Downloads URI: $uri")
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(content.toByteArray())
                outputStream.flush()
                Log.d(TAG, "Successfully wrote to Downloads folder")
            } ?: throw Exception("Failed to write to Downloads")
            
            return ExportResult.Success(
                filePath = "Downloads/$fileName",
                fileName = fileName,
                recordCount = recordCount,
                fileSize = content.length.toLong()
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "MediaStore save failed, trying legacy", e)
            return saveToDownloadsLegacy(context, fileName, content, recordCount)
        }
    }
    
    /**
     * Save to Downloads using legacy approach (Android 9 and below)
     */
    private fun saveToDownloadsLegacy(
        context: Context, 
        fileName: String, 
        content: String, 
        recordCount: Int
    ): ExportResult {
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            
            val exportFile = File(downloadsDir, fileName)
            Log.d(TAG, "Writing to legacy Downloads: ${exportFile.absolutePath}")
            
            FileWriter(exportFile).use { writer ->
                writer.write(content)
                writer.flush()
            }
            
            Log.d(TAG, "Successfully wrote to Downloads folder (legacy)")
            
            return ExportResult.Success(
                filePath = exportFile.absolutePath,
                fileName = fileName,
                recordCount = recordCount,
                fileSize = exportFile.length()
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Legacy Downloads save failed", e)
            throw e
        }
    }
    
    /**
     * Generate CSV content for all data
     */
    private fun generateCsvContent(
        dataPoints: List<DataPoint>,
        plugins: Map<String, Plugin>
    ): String {
        val csv = StringBuilder()
        
        // Headers
        val headers = listOf("Date", "Time", "Plugin", "Type", "Value", "Source", "Plugin_Specific_Data")
        csv.appendLine(headers.joinToString(",") { "\"$it\"" })
        
        // Group by plugin for better organization
        dataPoints.groupBy { it.pluginId }.forEach { (pluginId, pluginData) ->
            val plugin = plugins[pluginId]
            
            pluginData.forEach { dataPoint ->
                try {
                    val formatted = plugin?.formatForExport(dataPoint) ?: mapOf()
                    val localDateTime = LocalDateTime.ofInstant(dataPoint.timestamp, ZoneId.systemDefault())
                    
                    val row = listOf(
                        localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE),
                        localDateTime.format(DateTimeFormatter.ISO_LOCAL_TIME),
                        plugin?.metadata?.name ?: pluginId,
                        dataPoint.type,
                        dataPoint.value.toString(),
                        dataPoint.source ?: "unknown",
                        formatted.values.joinToString("; ")
                    )
                    
                    csv.appendLine(row.joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" })
                } catch (e: Exception) {
                    Log.e(TAG, "Error formatting data point ${dataPoint.id}", e)
                }
            }
        }
        
        return csv.toString()
    }
    
    /**
     * Generate CSV content for a specific plugin
     */
    private fun generatePluginCsvContent(
        dataPoints: List<DataPoint>,
        plugin: Plugin
    ): String {
        val csv = StringBuilder()
        
        // Use plugin-specific headers
        val headers = plugin.exportHeaders()
        csv.appendLine(headers.joinToString(",") { "\"$it\"" })
        
        // Format each data point using plugin-specific formatting
        dataPoints.forEach { dataPoint ->
            val formatted = plugin.formatForExport(dataPoint)
            val row = headers.map { header ->
                formatted[header] ?: ""
            }
            csv.appendLine(row.joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" })
        }
        
        return csv.toString()
    }
}

/**
 * Result of an export operation
 */
sealed class ExportResult {
    data class Success(
        val filePath: String,
        val fileName: String,
        val recordCount: Int,
        val fileSize: Long
    ) : ExportResult()
    
    data class Error(val message: String) : ExportResult()
}
