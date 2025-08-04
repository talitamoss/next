package com.domain.app.core.export

import android.content.Context
import android.os.Environment
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
    
    /**
     * Export all user data to CSV format
     */
    suspend fun exportAllDataToCsv(context: Context): ExportResult = withContext(Dispatchers.IO) {
        try {
            val allData = dataRepository.getRecentData(24 * 365).first()
            val plugins = pluginManager.getAllActivePlugins().associateBy { it.id }
            
            if (allData.isEmpty()) {
                return@withContext ExportResult.Error("No data to export")
            }
            
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
            val fileName = "behavioral_data_export_$timestamp.csv"
            
            val exportFile = createExportFile(context, fileName)
            val csvContent = generateCsvContent(allData, plugins)
            
            FileWriter(exportFile).use { writer ->
                writer.write(csvContent)
            }
            
            ExportResult.Success(
                filePath = exportFile.absolutePath,
                fileName = fileName,
                recordCount = allData.size,
                fileSize = exportFile.length()
            )
            
        } catch (e: Exception) {
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
        try {
            val plugin = pluginManager.getPlugin(pluginId)
                ?: return@withContext ExportResult.Error("Plugin not found: $pluginId")
            
            val data = if (startDate != null && endDate != null) {
                dataRepository.getPluginDataInRange(pluginId, startDate, endDate)
            } else {
                dataRepository.getPluginData(pluginId).first()
            }
            
            if (data.isEmpty()) {
                return@withContext ExportResult.Error("No data found for ${plugin.metadata.name}")
            }
            
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
            val fileName = "${plugin.metadata.name.lowercase().replace(" ", "_")}_export_$timestamp.csv"
            
            val exportFile = createExportFile(context, fileName)
            val csvContent = generatePluginCsvContent(data, plugin)
            
            FileWriter(exportFile).use { writer ->
                writer.write(csvContent)
            }
            
            ExportResult.Success(
                filePath = exportFile.absolutePath,
                fileName = fileName,
                recordCount = data.size,
                fileSize = exportFile.length()
            )
            
        } catch (e: Exception) {
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
        
        // Group by plugin for consistent formatting
        dataPoints.groupBy { it.pluginId }.forEach { (pluginId, pluginData) ->
            val plugin = plugins[pluginId]
            
            pluginData.forEach { dataPoint ->
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
     * Create export file in Downloads directory
     */
    private fun createExportFile(context: Context, fileName: String): File {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val appExportDir = File(downloadsDir, "BehavioralData")
        
        if (!appExportDir.exists()) {
            appExportDir.mkdirs()
        }
        
        return File(appExportDir, fileName)
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
