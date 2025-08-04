package com.domain.app.core.export

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
     * Export all user data to CSV format
     */
    suspend fun exportAllDataToCsv(context: Context): ExportResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "=== EXPORT DEBUG START ===")
        
        try {
            // Step 1: Check plugins
            Log.d(TAG, "Step 1: Getting active plugins...")
            val plugins = pluginManager.getAllActivePlugins()
            Log.d(TAG, "Found ${plugins.size} active plugins: ${plugins.map { "${it.id}(${it.metadata.name})" }}")
            
            if (plugins.isEmpty()) {
                Log.w(TAG, "ERROR: No active plugins found!")
                return@withContext ExportResult.Error("No active plugins found")
            }
            
            // Step 2: Get data from repository
            Log.d(TAG, "Step 2: Retrieving data from repository...")
            Log.d(TAG, "Calling dataRepository.getRecentData(${24 * 365}) - getting last 365 days")
            
            val allDataFlow = dataRepository.getRecentData(24 * 365)
            Log.d(TAG, "Got Flow, calling .first()...")
            
            val allData = allDataFlow.first()
            Log.d(TAG, "Retrieved ${allData.size} total data points")
            
            // Debug: Show breakdown by plugin
            val dataByPlugin = allData.groupBy { it.pluginId }
            dataByPlugin.forEach { (pluginId, pluginData) ->
                Log.d(TAG, "  Plugin $pluginId: ${pluginData.size} records")
                if (pluginData.isNotEmpty()) {
                    val latest = pluginData.maxByOrNull { it.timestamp }
                    val oldest = pluginData.minByOrNull { it.timestamp }
                    Log.d(TAG, "    Latest: ${latest?.timestamp}")
                    Log.d(TAG, "    Oldest: ${oldest?.timestamp}")
                    Log.d(TAG, "    Sample value: ${latest?.value}")
                }
            }
            
            if (allData.isEmpty()) {
                Log.w(TAG, "ERROR: No data found in database!")
                
                // Additional debugging - check each plugin individually
                Log.d(TAG, "Checking each plugin individually...")
                for (plugin in plugins) {
                    try {
                        val pluginDataFlow = dataRepository.getPluginData(plugin.id)
                        val pluginData = pluginDataFlow.first()
                        val dataCount = dataRepository.getDataCount(plugin.id)
                        Log.d(TAG, "  Plugin ${plugin.id}:")
                        Log.d(TAG, "    Flow result: ${pluginData.size} items")
                        Log.d(TAG, "    Count query: $dataCount items")
                    } catch (e: Exception) {
                        Log.e(TAG, "  Plugin ${plugin.id} data check failed", e)
                    }
                }
                
                return@withContext ExportResult.Error("No data to export - database appears empty")
            }
            
            // Step 3: Create export file
            Log.d(TAG, "Step 3: Creating export file...")
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
            val fileName = "behavioral_data_export_$timestamp.csv"
            Log.d(TAG, "Export filename: $fileName")
            
            val exportFile = createExportFile(context, fileName)
            Log.d(TAG, "Export file path: ${exportFile.absolutePath}")
            Log.d(TAG, "Parent directory exists: ${exportFile.parentFile?.exists()}")
            Log.d(TAG, "Parent directory writable: ${exportFile.parentFile?.canWrite()}")
            
            // Step 4: Generate CSV content
            Log.d(TAG, "Step 4: Generating CSV content...")
            val pluginMap = plugins.associateBy { it.id }
            val csvContent = generateCsvContent(allData, pluginMap)
            
            Log.d(TAG, "Generated CSV content:")
            Log.d(TAG, "  Length: ${csvContent.length} characters")
            Log.d(TAG, "  Line count: ${csvContent.lines().size}")
            Log.d(TAG, "  Preview (first 300 chars): ${csvContent.take(300)}")
            
            // Step 5: Write file using standard FileWriter
            Log.d(TAG, "Step 5: Writing file...")
            
            FileWriter(exportFile).use { writer ->
                writer.write(csvContent)
                writer.flush() // Ensure content is written
            }
            
            Log.d(TAG, "File write completed")
            
            // Step 6: Verify file creation
            val fileExists = exportFile.exists()
            val fileSize = exportFile.length()
            Log.d(TAG, "File created: $fileExists")
            Log.d(TAG, "File size: $fileSize bytes")
            
            if (!fileExists) {
                Log.e(TAG, "ERROR: File was not created!")
                return@withContext ExportResult.Error("Failed to create export file")
            }
            
            if (fileSize == 0L) {
                Log.e(TAG, "ERROR: File is empty!")
                return@withContext ExportResult.Error("Export file is empty")
            }
            
            Log.d(TAG, "=== EXPORT COMPLETED SUCCESSFULLY ===")
            
            ExportResult.Success(
                filePath = exportFile.absolutePath,
                fileName = fileName,
                recordCount = allData.size,
                fileSize = fileSize
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "EXPORT FAILED WITH EXCEPTION", e)
            Log.e(TAG, "Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "Exception message: ${e.message}")
            Log.e(TAG, "Stack trace: ${Log.getStackTraceString(e)}")
            
            ExportResult.Error("Export failed: ${e.message}")
        }
    }
    
    /**
     * Export data for a specific plugin
     */
    suspend fun exportPluginData(
        context: Context,
        pluginId: String,
        startDate: Instant? = null,
        endDate: Instant? = null
    ): ExportResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "=== PLUGIN EXPORT DEBUG: $pluginId ===")
        
        try {
            val plugin = pluginManager.getPlugin(pluginId)
            if (plugin == null) {
                Log.e(TAG, "Plugin not found: $pluginId")
                return@withContext ExportResult.Error("Plugin not found: $pluginId")
            }
            
            Log.d(TAG, "Plugin found: ${plugin.metadata.name}")
            
            val data = if (startDate != null && endDate != null) {
                Log.d(TAG, "Getting data in range: $startDate to $endDate")
                dataRepository.getPluginDataInRange(pluginId, startDate, endDate)
            } else {
                Log.d(TAG, "Getting all plugin data...")
                dataRepository.getPluginData(pluginId).first()
            }
            
            Log.d(TAG, "Retrieved ${data.size} data points for plugin")
            
            if (data.isEmpty()) {
                Log.w(TAG, "No data found for plugin ${plugin.metadata.name}")
                return@withContext ExportResult.Error("No data found for ${plugin.metadata.name}")
            }
            
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
            val fileName = "${plugin.metadata.name.lowercase().replace(" ", "_")}_export_$timestamp.csv"
            
            val exportFile = createExportFile(context, fileName)
            val csvContent = generatePluginCsvContent(data, plugin)
            
            Log.d(TAG, "Writing ${csvContent.length} characters to file")
            
            FileWriter(exportFile).use { writer ->
                writer.write(csvContent)
            }
            
            Log.d(TAG, "Plugin export completed successfully")
            
            ExportResult.Success(
                filePath = exportFile.absolutePath,
                fileName = fileName,
                recordCount = data.size,
                fileSize = exportFile.length()
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Plugin export failed", e)
            ExportResult.Error("Export failed: ${e.message}")
        }
    }
    
    /**
     * Generate CSV content for all data
     */
    private fun generateCsvContent(
        dataPoints: List<DataPoint>,
        plugins: Map<String, Plugin>
    ): String {
        Log.d(TAG, "Generating CSV for ${dataPoints.size} data points")
        
        val csv = StringBuilder()
        
        // Universal headers
        val headers = listOf(
            "Date",
            "Time", 
            "Plugin",
            "Type",
            "Value",
            "Source",
            "Plugin_Specific_Data"
        )
        
        csv.appendLine(headers.joinToString(",") { "\"$it\"" })
        Log.d(TAG, "Added CSV headers")
        
        // Group by plugin for consistent formatting
        val groupedData = dataPoints.groupBy { it.pluginId }
        Log.d(TAG, "Grouped data into ${groupedData.size} plugin groups")
        
        groupedData.forEach { (pluginId, pluginData) ->
            val plugin = plugins[pluginId]
            Log.d(TAG, "Processing ${pluginData.size} records for plugin $pluginId")
            
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
        
        Log.d(TAG, "CSV generation complete. Total length: ${csv.length}")
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
        
        val headers = plugin.exportHeaders()
        csv.appendLine(headers.joinToString(",") { "\"$it\"" })
        
        dataPoints.forEach { dataPoint ->
            val formatted = plugin.formatForExport(dataPoint)
            val row = headers.map { header ->
                formatted[header] ?: ""
            }
            csv.appendLine(row.joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" })
        }
        
        return csv.toString()
    }
    
    /**
     * Create export file using modern Android storage API
     */
    private fun createExportFile(context: Context, fileName: String): File {
        Log.d(TAG, "Creating export file: $fileName")
        Log.d(TAG, "Android SDK: ${Build.VERSION.SDK_INT}")
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Modern scoped storage approach for Android 10+
            Log.d(TAG, "Using app-specific storage for Android 10+")
            createExportFileAppSpecific(context, fileName)
        } else {
            // Legacy approach for older Android versions
            Log.d(TAG, "Using legacy file approach")
            createExportFileLegacy(context, fileName)
        }
    }
    
    /**
     * App-specific storage for Android 10+ (no permissions needed)
     */
    private fun createExportFileAppSpecific(context: Context, fileName: String): File {
        try {
            // Use app-specific external storage (no permissions required)
            val appExportDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "BehavioralData")
            Log.d(TAG, "App-specific export directory: ${appExportDir.absolutePath}")
            
            if (!appExportDir.exists()) {
                Log.d(TAG, "Creating app-specific export directory...")
                val created = appExportDir.mkdirs()
                Log.d(TAG, "Directory creation result: $created")
            }
            
            val exportFile = File(appExportDir, fileName)
            Log.d(TAG, "App-specific export file path: ${exportFile.absolutePath}")
            
            return exportFile
            
        } catch (e: Exception) {
            Log.e(TAG, "App-specific file creation failed, falling back to internal storage", e)
            
            // Fallback to internal storage
            val internalDir = File(context.filesDir, "exports")
            if (!internalDir.exists()) {
                internalDir.mkdirs()
            }
            return File(internalDir, fileName)
        }
    }
    
    /**
     * Legacy file creation for older Android versions
     */
    private fun createExportFileLegacy(context: Context, fileName: String): File {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        Log.d(TAG, "Downloads directory: ${downloadsDir?.absolutePath}")
        Log.d(TAG, "Downloads directory exists: ${downloadsDir?.exists()}")
        
        val appExportDir = File(downloadsDir, "BehavioralData")
        Log.d(TAG, "App export directory: ${appExportDir.absolutePath}")
        
        if (!appExportDir.exists()) {
            Log.d(TAG, "Creating app export directory...")
            val created = appExportDir.mkdirs()
            Log.d(TAG, "Directory creation result: $created")
        }
        
        val exportFile = File(appExportDir, fileName)
        Log.d(TAG, "Final export file path: ${exportFile.absolutePath}")
        
        return exportFile
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
