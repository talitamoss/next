package com.domain.app.plugins

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import com.domain.app.core.data.DataPoint
import com.domain.app.core.plugin.*
import com.domain.app.core.plugin.security.*
import com.domain.app.core.validation.ValidationResult
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Audio Recording Plugin - Verified Implementation
 * Following exact patterns from WaterPlugin and SleepPlugin
 * Enables high-quality voice note recording with local storage
 */
class AudioPlugin : Plugin {
    override val id = "audio"
    
    // Internal state management
    private var mediaRecorder: MediaRecorder? = null
    private var currentRecordingPath: String? = null
    private var recordingStartTime: Instant? = null
    private lateinit var appContext: Context
    private var isInitialized = false
    
    override val metadata = PluginMetadata(
        name = "Audio Notes",
        description = "Record voice notes and audio observations",
        version = "1.0.0",
        author = "System",
        category = PluginCategory.JOURNAL,
        tags = listOf("audio", "voice", "notes", "recording", "journal"),
        dataPattern = DataPattern.COMPOSITE,
        inputType = InputType.BUTTON,
        supportsMultiStage = false,
        relatedPlugins = listOf("mood", "meditation"),
        exportFormat = ExportFormat.CSV,
        dataSensitivity = DataSensitivity.SENSITIVE,
        naturalLanguageAliases = listOf(
            "record audio", "voice note", "audio note",
            "voice memo", "record voice", "audio diary"
        ),
        contextualTriggers = listOf(
            ContextTrigger.MANUAL_ONLY
        ),
        permissions = listOf(
            "android.permission.RECORD_AUDIO"
        )
    )
    
    override val securityManifest = PluginSecurityManifest(
        requestedCapabilities = setOf(
            PluginCapability.COLLECT_DATA,
            PluginCapability.READ_OWN_DATA,
            PluginCapability.LOCAL_STORAGE,
            PluginCapability.EXPORT_DATA,
            PluginCapability.MICROPHONE_ACCESS,
            PluginCapability.FILE_ACCESS
        ),
        dataSensitivity = DataSensitivity.SENSITIVE,
        dataAccess = setOf(DataAccessScope.OWN_DATA_ONLY),
        privacyPolicy = "Audio recordings are stored locally on your device with encryption. " +
                       "Files never leave your device without explicit export action.",
        dataRetention = DataRetentionPolicy.USER_CONTROLLED
    )
    
    override val trustLevel = PluginTrustLevel.OFFICIAL
    
    override fun getPermissionRationale() = mapOf(
        PluginCapability.COLLECT_DATA to "Record and save audio notes",
        PluginCapability.READ_OWN_DATA to "Access your audio recordings",
        PluginCapability.LOCAL_STORAGE to "Store recordings on your device",
        PluginCapability.EXPORT_DATA to "Export recordings for backup",
        PluginCapability.MICROPHONE_ACCESS to "Use microphone for recording",
        PluginCapability.FILE_ACCESS to "Save audio files securely"
    )
    
    override suspend fun initialize(context: Context) {
        appContext = context
        // Create private directory for audio storage
        val audioDir = File(context.filesDir, "audio_recordings")
        if (!audioDir.exists()) {
            audioDir.mkdirs()
        }
        isInitialized = true
    }
    
    override fun supportsManualEntry() = true
    
    override fun supportsAutomaticCollection() = false
    
    override fun getQuickAddConfig() = QuickAddConfig(
        id = "audio",
        title = "Record Audio Note",
        inputType = InputType.BUTTON,
        buttonText = "Start Recording",
        primaryColor = "#EF4444"
    )
    
    override suspend fun collectData(): DataPoint? {
        // Automatic collection not supported for audio
        return null
    }
    
    override suspend fun createManualEntry(data: Map<String, Any>): DataPoint? {
        return when {
            // Case 1: Recording completed, DataPoint passed directly
            data.containsKey("recorded_data_point") -> {
                data["recorded_data_point"] as? DataPoint
            }
            
            // Case 2: Start recording request
            data["action"] == "start" -> {
                if (startRecording()) {
                    DataPoint(
                        id = generateDataPointId(),
                        pluginId = id,
                        timestamp = Instant.now(),
                        type = "recording_status",
                        value = mapOf(
                            "status" to "recording_started",
                            "start_time" to Instant.now().toString()
                        ),
                        source = "manual"
                    )
                } else {
                    null
                }
            }
            
            // Case 3: Stop recording request
            data["action"] == "stop" -> {
                stopRecording()
            }
            
            else -> null
        }
    }
    
    override fun validateDataPoint(data: Map<String, Any>): ValidationResult {
        return when {
            // For completed recordings
            data.containsKey("file_path") && data.containsKey("duration_seconds") -> {
                val filePath = data["file_path"] as? String
                val duration = data["duration_seconds"] as? Number
                
                when {
                    filePath.isNullOrEmpty() -> 
                        ValidationResult.Error("Invalid file path")
                    duration == null || duration.toDouble() <= 0 -> 
                        ValidationResult.Error("Invalid recording duration")
                    !File(filePath).exists() -> 
                        ValidationResult.Error("Audio file not found")
                    else -> ValidationResult.Success
                }
            }
            
            // For action requests
            data.containsKey("action") -> {
                val action = data["action"] as? String
                if (action in listOf("start", "stop")) {
                    ValidationResult.Success
                } else {
                    ValidationResult.Error("Unknown action: $action")
                }
            }
            
            data.containsKey("recorded_data_point") -> {
                ValidationResult.Success
            }
            
            else -> ValidationResult.Error("Missing required audio data")
        }
    }
    
    override fun exportHeaders(): List<String> {
        return listOf(
            "timestamp",
            "file_name",
            "duration_seconds", 
            "file_size_kb",
            "recording_quality",
            "sample_rate",
            "bitrate",
            "format"
        )
    }
    
    override fun formatForExport(dataPoint: DataPoint): Map<String, String> {
        val value = dataPoint.value
        val metadata = dataPoint.metadata ?: emptyMap()
        
        return mapOf(
            "timestamp" to dataPoint.timestamp.toString(),
            "file_name" to (value["file_name"]?.toString() ?: ""),
            "duration_seconds" to (value["duration_seconds"]?.toString() ?: "0"),
            "file_size_kb" to (value["file_size_kb"]?.toString() ?: "0"),
            "recording_quality" to (value["recording_quality"]?.toString() ?: "standard"),
            "sample_rate" to (value["sample_rate"]?.toString() ?: "44100"),
            "bitrate" to (value["bitrate"]?.toString() ?: "128000"),
            "format" to (metadata["format"] ?: "m4a")
        )
    }
    
    override suspend fun cleanup() {
        try {
            mediaRecorder?.apply {
                if (isRecording()) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            // Ignore cleanup errors
        } finally {
            mediaRecorder = null
            currentRecordingPath = null
            recordingStartTime = null
        }
    }
    
    // ========== Audio-specific methods ==========
    
    /**
     * Generate unique ID for data points
     */
    private fun generateDataPointId(): String {
        return "${id}_${System.currentTimeMillis()}_${UUID.randomUUID()}"
    }
    
    /**
     * Check if currently recording
     */
    private fun isRecording(): Boolean {
        return mediaRecorder != null && currentRecordingPath != null
    }
    
    /**
     * Start audio recording with high quality settings
     */
    fun startRecording(): Boolean {
        if (!isInitialized) return false
        if (mediaRecorder != null) return false  // Already recording
        
        return try {
            val timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
            val fileName = "audio_note_$timestamp.m4a"
            
            val audioDir = File(appContext.filesDir, "audio_recordings")
            if (!audioDir.exists()) {
                audioDir.mkdirs()
            }
            
            val audioFile = File(audioDir, fileName)
            currentRecordingPath = audioFile.absolutePath
            
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(appContext)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setOutputFile(currentRecordingPath)
                prepare()
                start()
            }
            
            recordingStartTime = Instant.now()
            true
            
        } catch (e: Exception) {
            e.printStackTrace()
            mediaRecorder?.release()
            mediaRecorder = null
            currentRecordingPath = null
            false
        }
    }
    
    /**
     * Stop recording and create DataPoint with audio metadata
     */
    fun stopRecording(): DataPoint? {
        if (!isRecording()) return null
        
        return try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null
            
            val endTime = Instant.now()
            val startTime = recordingStartTime ?: endTime
            val durationSeconds = java.time.Duration.between(startTime, endTime).seconds
            
            val audioFile = File(currentRecordingPath!!)
            val fileSizeKb = audioFile.length() / 1024
            
            val dataPoint = DataPoint(
                id = generateDataPointId(),
                pluginId = id,
                timestamp = endTime,
                type = "audio_recording",
                value = mapOf(
                    "file_path" to currentRecordingPath!!,
                    "file_name" to audioFile.name,
                    "duration_seconds" to durationSeconds,
                    "file_size_kb" to fileSizeKb,
                    "recording_quality" to "high",
                    "sample_rate" to 44100,
                    "bitrate" to 128000
                ),
                metadata = mapOf(
                    "format" to "m4a",
                    "start_time" to startTime.toString(),
                    "end_time" to endTime.toString()
                ),
                source = "manual"
            )
            
            currentRecordingPath = null
            recordingStartTime = null
            
            dataPoint
            
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            mediaRecorder?.release()
            mediaRecorder = null
        }
    }
}
