// app/src/main/java/com/domain/app/ui/settings/FeedbackDialog.kt
package com.domain.app.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.domain.app.BuildConfig

/**
 * Feedback types for categorization
 */
enum class FeedbackType(val label: String) {
    BUG("Bug Report"),
    FEATURE("Feature Request"),
    GENERAL("General Feedback"),
    CRASH("Crash Report")
}

/**
 * Simple feedback dialog with optional crash reporting
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackDialog(
    onDismiss: () -> Unit,
    feedbackEmail: String = "feedback@yourdomain.com",
    crashLog: String? = null,
    preselectedType: FeedbackType? = null
) {
    var feedbackType by remember { mutableStateOf(preselectedType ?: FeedbackType.GENERAL) }
    var feedbackText by remember { mutableStateOf("") }
    var includeDeviceInfo by remember { mutableStateOf(true) }
    var includeCrashLog by remember { mutableStateOf(crashLog != null) }
    
    val context = LocalContext.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = when (feedbackType) {
                    FeedbackType.BUG -> Icons.Default.BugReport
                    FeedbackType.CRASH -> Icons.Default.Warning
                    FeedbackType.FEATURE -> Icons.Default.Lightbulb
                    FeedbackType.GENERAL -> Icons.Default.Feedback
                },
                contentDescription = null,
                tint = if (feedbackType == FeedbackType.CRASH) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
        },
        title = {
            Text("Send Feedback")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Crash alert if present
                if (crashLog != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Column {
                                Text(
                                    "App Crash Detected",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    "Please describe what you were doing when it occurred.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
                
                // Feedback type selection
                Text(
                    "Type of feedback:",
                    style = MaterialTheme.typography.labelMedium
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FeedbackType.values().forEach { type ->
                        FilterChip(
                            selected = feedbackType == type,
                            onClick = { feedbackType = type },
                            label = { Text(type.label.split(" ").last()) },
                            enabled = type != FeedbackType.CRASH || crashLog != null
                        )
                    }
                }
                
                // Feedback text input
                OutlinedTextField(
                    value = feedbackText,
                    onValueChange = { feedbackText = it },
                    label = { Text("Your feedback") },
                    placeholder = {
                        Text(
                            when (feedbackType) {
                                FeedbackType.BUG -> "Describe what went wrong..."
                                FeedbackType.FEATURE -> "What feature would you like?"
                                FeedbackType.CRASH -> "What were you doing when the app crashed?"
                                FeedbackType.GENERAL -> "Share your thoughts..."
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )
                
                // Include options
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = includeDeviceInfo,
                            onCheckedChange = { includeDeviceInfo = it }
                        )
                        Text(
                            "Include device information",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    
                    if (crashLog != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = includeCrashLog,
                                onCheckedChange = { includeCrashLog = it }
                            )
                            Text(
                                "Include crash log (${crashLog.lines().size} lines)",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val fullFeedback = buildFeedbackMessage(
                        feedbackType = feedbackType,
                        feedbackText = feedbackText,
                        includeDeviceInfo = includeDeviceInfo,
                        includeCrashLog = includeCrashLog,
                        crashLog = crashLog
                    )
                    
                    sendFeedbackEmail(
                        context = context,
                        email = feedbackEmail,
                        subject = "${feedbackType.label} - App v${BuildConfig.VERSION_NAME}",
                        body = fullFeedback
                    )
                    onDismiss()
                },
                enabled = feedbackText.isNotBlank()
            ) {
                Text("Send")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Build the complete feedback message
 */
private fun buildFeedbackMessage(
    feedbackType: FeedbackType,
    feedbackText: String,
    includeDeviceInfo: Boolean,
    includeCrashLog: Boolean,
    crashLog: String?
): String {
    return buildString {
        appendLine("Type: ${feedbackType.label}")
        appendLine()
        appendLine("Message:")
        appendLine(feedbackText)
        
        if (includeDeviceInfo) {
            appendLine()
            appendLine("---")
            appendLine("Device Information:")
            appendLine("App Version: ${BuildConfig.VERSION_NAME}")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Android Version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
        }
        
        if (includeCrashLog && crashLog != null) {
            appendLine()
            appendLine("---")
            appendLine("Crash Log:")
            appendLine(crashLog)
        }
    }
}

/**
 * Send feedback via email intent
 */
private fun sendFeedbackEmail(
    context: Context,
    email: String,
    subject: String,
    body: String
) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
    }
    
    try {
        context.startActivity(Intent.createChooser(intent, "Send feedback via..."))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(
            context,
            "No email app found. Please send feedback to: $email",
            Toast.LENGTH_LONG
        ).show()
    }
}

/**
 * Crash Handler to capture uncaught exceptions
 * This is a simple implementation that saves the last crash to SharedPreferences
 */
class CrashHandler(
    private val context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {
    
    companion object {
        private const val CRASH_LOG_KEY = "last_crash_log"
        private const val CRASH_TIME_KEY = "last_crash_time"
        private const val PREFS_NAME = "crash_logs"
        
        /**
         * Install the crash handler
         */
        fun install(context: Context) {
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler(
                CrashHandler(context, defaultHandler)
            )
        }
        
        /**
         * Get the last saved crash log if any
         */
        fun getLastCrashLog(context: Context): String? {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(CRASH_LOG_KEY, null)
        }
        
        /**
         * Clear the saved crash log
         */
        fun clearCrashLog(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .remove(CRASH_LOG_KEY)
                .remove(CRASH_TIME_KEY)
                .apply()
        }
    }
    
    override fun uncaughtException(thread: Thread, exception: Throwable) {
        try {
            // Build crash log
            val crashLog = buildString {
                appendLine("Crash occurred at: ${java.util.Date()}")
                appendLine("Thread: ${thread.name}")
                appendLine()
                appendLine("Exception: ${exception.javaClass.name}")
                appendLine("Message: ${exception.message ?: "No message"}")
                appendLine()
                appendLine("Stack trace:")
                exception.stackTraceToString().lines().take(30).forEach { line ->
                    appendLine(line)
                }
            }
            
            // Save crash log to SharedPreferences
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(CRASH_LOG_KEY, crashLog)
                .putLong(CRASH_TIME_KEY, System.currentTimeMillis())
                .apply()
            
        } catch (e: Exception) {
            // Ignore errors while saving crash log
        }
        
        // Pass to default handler (which will show the crash dialog)
        defaultHandler?.uncaughtException(thread, exception)
    }
}
