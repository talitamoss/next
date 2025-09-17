// FeedbackDialog.kt - Add this as a new file in your ui/settings folder
package com.domain.app.ui.settings

import android.app.ActivityManager
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.domain.app.BuildConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class FeedbackType(val label: String) {
    BUG("Bug Report"),
    FEATURE("Feature Request"),
    GENERAL("General Feedback"),
    COMPLIMENT("Compliment"),
    CRASH("Crash Report")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackDialog(
    onDismiss: () -> Unit,
    feedbackEmail: String = "feedback@yourdomain.com", // Replace with your email
    crashLog: String? = null, // Pass crash log if available
    preselectedType: FeedbackType? = null // Pre-select crash type if crash occurred
) {
    var feedbackType by remember { mutableStateOf(preselectedType ?: FeedbackType.GENERAL) }
    var feedbackText by remember { mutableStateOf("") }
    var userEmail by remember { mutableStateOf("") }
    var includeDeviceInfo by remember { mutableStateOf(true) }
    var includeCrashLog by remember { mutableStateOf(crashLog != null) }
    var sendMethod by remember { mutableStateOf(SendMethod.EMAIL_APP) }
    var showSuccess by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Build device info string
    val deviceInfo = remember {
        """
        
        ---
        Device Information:
        App Version: ${BuildConfig.VERSION_NAME}
        Device: ${Build.MANUFACTURER} ${Build.MODEL}
        Android Version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})
        Available Memory: ${getAvailableMemory(context)}
        Total Storage: ${getTotalStorage()}
        """.trimIndent()
    }
    
    if (showSuccess) {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    "Feedback Prepared!",
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    when (sendMethod) {
                        SendMethod.EMAIL_APP -> "Your email app will open with your feedback ready to send."
                        SendMethod.COPY -> "Your feedback has been copied to clipboard. Paste it in your preferred email app."
                    },
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Done")
                }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            properties = DialogProperties(usePlatformDefaultWidth = false),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Feedback,
                        contentDescription = null
                    )
                    Text("Send Feedback")
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Feedback Type Selection
                    Text(
                        text = "What kind of feedback?",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FeedbackType.values().take(2).forEach { type ->
                            FilterChip(
                                selected = feedbackType == type,
                                onClick = { feedbackType = type },
                                label = {
                                    Text(type.label.split(" ").last())
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FeedbackType.values().drop(2).forEach { type ->
                            FilterChip(
                                selected = feedbackType == type,
                                onClick = { feedbackType = type },
                                label = {
                                    Text(type.label.split(" ").last())
                                },
                                modifier = Modifier.weight(1f),
                                enabled = type != FeedbackType.CRASH || crashLog != null
                            )
                        }
                    }
                    
                    // Feedback Text Input
                    OutlinedTextField(
                        value = feedbackText,
                        onValueChange = { feedbackText = it },
                        label = { Text("Your feedback") },
                        placeholder = {
                            Text(
                                when (feedbackType) {
                                    FeedbackType.BUG -> "Describe what went wrong..."
                                    FeedbackType.FEATURE -> "What feature would you like to see?"
                                    FeedbackType.COMPLIMENT -> "What do you love about the app?"
                                    FeedbackType.GENERAL -> "Share your thoughts..."
                                }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 200.dp),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Default
                        ),
                        minLines = 5,
                        maxLines = 10
                    )
                    
                    // Optional: User Email
                    OutlinedTextField(
                        value = userEmail,
                        onValueChange = { userEmail = it },
                        label = { Text("Your email (optional)") },
                        placeholder = { Text("For follow-up questions") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done
                        ),
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                    
                    // Include Device Info Checkbox
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = includeDeviceInfo,
                            onCheckedChange = { includeDeviceInfo = it }
                        )
                        Text(
                            text = "Include device information",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = { /* Show device info preview */ },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Device info details",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // Include Crash Log Checkbox (if available)
                    if (crashLog != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = includeCrashLog,
                                onCheckedChange = { includeCrashLog = it }
                            )
                            Text(
                                text = "Include crash log",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "${crashLog.lines().size} lines",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // Send Method Selection
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "How to send:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RadioButton(
                                    selected = sendMethod == SendMethod.EMAIL_APP,
                                    onClick = { sendMethod = SendMethod.EMAIL_APP }
                                )
                                Text(
                                    text = "Open email app",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RadioButton(
                                    selected = sendMethod == SendMethod.COPY,
                                    onClick = { sendMethod = SendMethod.COPY }
                                )
                                Text(
                                    text = "Copy to clipboard",
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
                        val fullFeedback = buildString {
                            if (userEmail.isNotBlank()) {
                                appendLine("From: $userEmail")
                                appendLine()
                            }
                            
                            appendLine(feedbackText)
                            
                            if (includeDeviceInfo) {
                                appendLine()
                                append(deviceInfo)
                            }
                            
                            if (includeCrashLog && crashLog != null) {
                                appendLine()
                                appendLine("---")
                                appendLine("Crash Log:")
                                append(crashLog)
                            }
                        }
                        
                        when (sendMethod) {
                            SendMethod.EMAIL_APP -> {
                                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:")
                                    putExtra(Intent.EXTRA_EMAIL, arrayOf(feedbackEmail))
                                    putExtra(
                                        Intent.EXTRA_SUBJECT, 
                                        "${feedbackType.label} - App v${BuildConfig.VERSION_NAME}"
                                    )
                                    putExtra(Intent.EXTRA_TEXT, fullFeedback)
                                }
                                
                                try {
                                    context.startActivity(
                                        Intent.createChooser(emailIntent, "Send feedback via")
                                    )
                                    scope.launch {
                                        showSuccess = true
                                        delay(2000)
                                        onDismiss()
                                    }
                                } catch (e: ActivityNotFoundException) {
                                    // No email app available, fallback to copy
                                    sendMethod = SendMethod.COPY
                                    copyToClipboard(context, fullFeedback, feedbackEmail)
                                    showSuccess = true
                                }
                            }
                            
                            SendMethod.COPY -> {
                                copyToClipboard(context, fullFeedback, feedbackEmail)
                                showSuccess = true
                            }
                        }
                    },
                    enabled = feedbackText.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Send Feedback")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}

private enum class SendMethod {
    EMAIL_APP, COPY
}

private fun copyToClipboard(context: Context, text: String, email: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Feedback", text)
    clipboard.setPrimaryClip(clip)
    
    Toast.makeText(
        context,
        "Feedback copied! Send to: $email",
        Toast.LENGTH_LONG
    ).show()
}

// Helper functions for device info
private fun getAvailableMemory(context: Context): String {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memoryInfo = ActivityManager.MemoryInfo()
    activityManager.getMemoryInfo(memoryInfo)
    val availableMemory = memoryInfo.availMem / (1024 * 1024) // Convert to MB
    val totalMemory = memoryInfo.totalMem / (1024 * 1024) // Convert to MB
    return "${availableMemory}MB / ${totalMemory}MB"
}

private fun getTotalStorage(): String {
    val stat = StatFs(Environment.getDataDirectory().path)
    val available = (stat.availableBlocksLong * stat.blockSizeLong) / (1024 * 1024 * 1024) // GB
    val total = (stat.blockCountLong * stat.blockSizeLong) / (1024 * 1024 * 1024) // GB
    return "${available}GB / ${total}GB"
}

// Crash Handler class - Add this to your App.kt or a separate file
class CrashHandler(
    private val context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {
    
    companion object {
        private const val CRASH_LOG_KEY = "last_crash_log"
        private const val CRASH_TIME_KEY = "last_crash_time"
        
        fun install(context: Context) {
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler(
                CrashHandler(context, defaultHandler)
            )
        }
        
        fun getLastCrashLog(context: Context): String? {
            val prefs = context.getSharedPreferences("crash_logs", Context.MODE_PRIVATE)
            return prefs.getString(CRASH_LOG_KEY, null)
        }
        
        fun clearCrashLog(context: Context) {
            val prefs = context.getSharedPreferences("crash_logs", Context.MODE_PRIVATE)
            prefs.edit().remove(CRASH_LOG_KEY).remove(CRASH_TIME_KEY).apply()
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
                appendLine("Message: ${exception.message}")
                appendLine()
                appendLine("Stack trace:")
                exception.stackTraceToString().lines().take(50).forEach { line ->
                    appendLine(line)
                }
            }
            
            // Save crash log to SharedPreferences
            val prefs = context.getSharedPreferences("crash_logs", Context.MODE_PRIVATE)
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

// Add this to your SettingsScreen.kt file:
// At the top of the composable, add:
var showFeedbackDialog by remember { mutableStateOf(false) }
val context = LocalContext.current

// Check for crash logs on screen load
LaunchedEffect(Unit) {
    val crashLog = CrashHandler.getLastCrashLog(context)
    if (crashLog != null) {
        // Show dialog to report crash
        showFeedbackDialog = true
    }
}

// In the Information section, update the Feedback item:
SettingsItem(
    icon = Icons.Default.Feedback,
    title = "Send Feedback",
    subtitle = "Report bugs or suggest features",
    onClick = { showFeedbackDialog = true },
    enabled = true
)

// At the end of the SettingsScreen composable, add:
if (showFeedbackDialog) {
    val crashLog = CrashHandler.getLastCrashLog(context)
    
    FeedbackDialog(
        onDismiss = { 
            showFeedbackDialog = false
            // Clear crash log after user dismisses (whether they sent it or not)
            if (crashLog != null) {
                CrashHandler.clearCrashLog(context)
            }
        },
        feedbackEmail = "your-email@example.com", // Replace with your actual email
        crashLog = crashLog,
        preselectedType = if (crashLog != null) FeedbackType.CRASH else null
    )
}

// Add this to your App.kt in the onCreate() method:
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Install crash handler
        CrashHandler.install(this)
        
        // Rest of your initialization...
    }
}
