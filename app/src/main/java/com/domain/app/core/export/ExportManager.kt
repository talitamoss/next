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
import com.domain.app.core.plugin.ExportFormat
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
        Log.d(TAG, "Exporting plugin data: $pluginId")
        
        try {
            val plugin = pluginManager.getPlugin(pluginId)
                ?: return@withContext ExportResult.Error("Plugin not found: $pluginId")
            
            // Use suspend function if date range specified, otherwise use Flow
            val data = if (startDate != null && endDate != null) {
                // getPluginDataInRange returns List<DataPoint> directly (suspend function)
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
     * Export filtered data based on options
     * ENHANCED: Supports plugin filtering, date ranges, and multiple formats
     */
    suspend fun exportFilteredData(
        context: Context,
        format: ExportFormat,
        pluginIds: Set<String>? = null,
        startDate: Instant? = null,
        endDate: Instant? = null,
        encrypt: Boolean = false
    ): ExportResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "=== EXPORT FILTERED DATA START ===")
        Log.d(TAG, "Format: $format, Plugins: ${pluginIds?.size ?: "all"}, Encrypt: $encrypt")
        
        try {
            // Step 1: Get filtered data
            val allData = if (startDate != null && endDate != null) {
                dataRepository.getDataInRange(startDate, endDate).first()
            } else {
                dataRepository.getRecentData(24 * 365).first()
            }
            
            // Filter by plugins if specified
            val filteredData = if (!pluginIds.isNullOrEmpty()) {
                allData.filter { it.pluginId in pluginIds }
            } else {
                allData
            }
            
            Log.d(TAG, "Retrieved ${filteredData.size} data points after filtering")
            
            if (filteredData.isEmpty()) {
                return@withContext ExportResult.Error("No data to export for selected filters")
            }
            
            // Step 2: Generate content based on format
            val plugins = pluginManager.getAllActivePlugins()
            val pluginMap = plugins.associateBy { it.id }
            
            val (content, mimeType) = when (format) {
                ExportFormat.CSV -> generateCsvContent(filteredData, pluginMap) to "text/csv"
                ExportFormat.JSON -> generateJsonContent(filteredData, pluginMap) to "application/json"
                ExportFormat.XML -> generateXmlContent(filteredData, pluginMap) to "application/xml"
                ExportFormat.CUSTOM -> generateCsvContent(filteredData, pluginMap) to "text/plain"
            }
            
            // Step 3: Apply encryption if requested
            val finalContent = if (encrypt) {
                // TODO: Implement encryption
                Log.d(TAG, "Encryption requested but not yet implemented")
                content
            } else {
                content
            }
            
            // Step 4: Generate filename with descriptive parts
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
            val extension = when (format) {
                ExportFormat.CSV -> "csv"
                ExportFormat.JSON -> "json"
                ExportFormat.XML -> "xml"
                ExportFormat.CUSTOM -> "txt"
            }
            
            val pluginPart = when {
                pluginIds == null || pluginIds.isEmpty() -> "all_data"
                pluginIds.size == 1 -> pluginMap[pluginIds.first()]?.metadata?.name?.lowercase()?.replace(" ", "_") ?: "data"
                else -> "${pluginIds.size}_plugins"
            }
            
            val timePart = when {
                startDate != null && endDate != null -> {
                    val days = java.time.Duration.between(startDate, endDate).toDays()
                    when {
                        days <= 1 -> "day"
                        days <= 7 -> "week"
                        days <= 31 -> "month"
                        days <= 365 -> "year"
                        else -> "custom"
                    }
                }
                else -> "all_time"
            }
            
            val fileName = "${pluginPart}_${timePart}_export_$timestamp.$extension"
            
            // Step 5: Save to Downloads
            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Log.d(TAG, "Using MediaStore for Downloads folder")
                saveToDownloadsMediaStore(context, fileName, finalContent, filteredData.size, mimeType)
            } else {
                Log.d(TAG, "Using legacy Downloads approach")
                saveToDownloadsLegacy(context, fileName, finalContent, filteredData.size)
            }
            
            Log.d(TAG, "=== EXPORT FILTERED DATA COMPLETED ===")
            return@withContext result
            
        } catch (e: Exception) {
            Log.e(TAG, "Export failed", e)
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
        recordCount: Int,
        mimeType: String = "text/csv"
    ): ExportResult {
        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
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
    
    /**
     * Generate JSON content for export
     * NEW: Added JSON format support
     */
    private fun generateJsonContent(
        dataPoints: List<DataPoint>,
        plugins: Map<String, Plugin>
    ): String {
        val json = StringBuilder()
        json.append("{\n")
        json.append("  \"export_date\": \"${LocalDateTime.now()}\",\n")
        json.append("  \"total_records\": ${dataPoints.size},\n")
        json.append("  \"data\": [\n")
        
        dataPoints.forEachIndexed { index, dataPoint ->
            val plugin = plugins[dataPoint.pluginId]
            val localDateTime = LocalDateTime.ofInstant(dataPoint.timestamp, ZoneId.systemDefault())
            
            json.append("    {\n")
            json.append("      \"id\": \"${dataPoint.id}\",\n")
            json.append("      \"plugin\": \"${plugin?.metadata?.name ?: dataPoint.pluginId}\",\n")
            json.append("      \"date\": \"${localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE)}\",\n")
            json.append("      \"time\": \"${localDateTime.format(DateTimeFormatter.ISO_LOCAL_TIME)}\",\n")
            json.append("      \"type\": \"${dataPoint.type}\",\n")
            
            // Handle value as a complex object
            json.append("      \"value\": ")
            when (val value = dataPoint.value) {
                is Map<*, *> -> {
                    json.append("{")
                    value.entries.forEachIndexed { i, entry ->
                        json.append("\"${entry.key}\": ")
                        when (val v = entry.value) {
                            is String -> json.append("\"$v\"")
                            is Number -> json.append(v)
                            is Boolean -> json.append(v)
                            else -> json.append("\"${v.toString()}\"")
                        }
                        if (i < value.size - 1) json.append(", ")
                    }
                    json.append("}")
                }
                else -> json.append("\"${value}\"")
            }
            json.append(",\n")
            
            json.append("      \"source\": \"${dataPoint.source ?: "unknown"}\",\n")
            json.append("      \"metadata\": ")
            
            // Add plugin-specific formatted data
            val formatted = plugin?.formatForExport(dataPoint) ?: mapOf()
            json.append("{")
            formatted.entries.forEachIndexed { i, entry ->
                json.append("\"${entry.key}\": \"${entry.value}\"")
                if (i < formatted.size - 1) json.append(", ")
            }
            json.append("}\n")
            
            json.append("    }")
            
            if (index < dataPoints.size - 1) {
                json.append(",")
            }
            json.append("\n")
        }
        
        json.append("  ]\n")
        json.append("}\n")
        
        return json.toString()
    }
    
    /**
     * Generate XML content for export
     * NEW: Added XML format support
     */
    private fun generateXmlContent(
        dataPoints: List<DataPoint>,
        plugins: Map<String, Plugin>
    ): String {
        val xml = StringBuilder()
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        xml.append("<export>\n")
        xml.append("  <metadata>\n")
        xml.append("    <export_date>${LocalDateTime.now()}</export_date>\n")
        xml.append("    <total_records>${dataPoints.size}</total_records>\n")
        xml.append("  </metadata>\n")
        xml.append("  <data>\n")
        
        dataPoints.forEach { dataPoint ->
            val plugin = plugins[dataPoint.pluginId]
            val localDateTime = LocalDateTime.ofInstant(dataPoint.timestamp, ZoneId.systemDefault())
            
            xml.append("    <record>\n")
            xml.append("      <id>${escapeXml(dataPoint.id)}</id>\n")
            xml.append("      <plugin>${escapeXml(plugin?.metadata?.name ?: dataPoint.pluginId)}</plugin>\n")
            xml.append("      <date>${localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE)}</date>\n")
            xml.append("      <time>${localDateTime.format(DateTimeFormatter.ISO_LOCAL_TIME)}</time>\n")
            xml.append("      <type>${escapeXml(dataPoint.type)}</type>\n")
            
            // Handle value as complex object
            xml.append("      <value>\n")
            when (val value = dataPoint.value) {
                is Map<*, *> -> {
                    value.forEach { (k, v) ->
                        xml.append("        <${escapeXml(k.toString())}>${escapeXml(v.toString())}</${escapeXml(k.toString())}>\n")
                    }
                }
                else -> xml.append("        <data>${escapeXml(value.toString())}</data>\n")
            }
            xml.append("      </value>\n")
            
            xml.append("      <source>${escapeXml(dataPoint.source ?: "unknown")}</source>\n")
            
            // Add plugin-specific formatted data
            val formatted = plugin?.formatForExport(dataPoint) ?: mapOf()
            if (formatted.isNotEmpty()) {
                xml.append("      <plugin_data>\n")
                formatted.forEach { (key, value) ->
                    xml.append("        <${escapeXml(key)}>${escapeXml(value)}</${escapeXml(key)}>\n")
                }
                xml.append("      </plugin_data>\n")
            }
            
            xml.append("    </record>\n")
        }
        
        xml.append("  </data>\n")
        xml.append("</export>\n")
        
        return xml.toString()
    }
    
    /**
     * Escape special XML characters
     */
    private fun escapeXml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
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
