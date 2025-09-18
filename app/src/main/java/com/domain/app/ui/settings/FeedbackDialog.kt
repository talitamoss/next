// app/src/main/java/com/domain/app/ui/settings/FeedbackDialog.kt
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

enum class SendMethod {
    EMAIL_APP,
    COPY
}

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
    var userEmail by remember { mutableStateOf("") }
    var includeDeviceInfo by remember { mutableStateOf(true) }
    var includeCrashLog by remember { mutableStateOf(crashLog != null) }
    var sendMethod by remember { mutableStateOf(SendMethod.EMAIL_APP) }
    var showSuccess by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Build device info string
    val deviceInfo = remember {
        buildString {
            appendLine()
            appendLine("---")
            appendLine("Device Information:")
            appendLine("App Version: ${BuildConfig.VERSION_NAME}")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Android Version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            appendLine("Available Memory: ${getAvailableMemory(context)}")
            appendLine("Total Storage: ${getTotalStorage()}")
        }
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
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("OK")
                }
            }
        )
        return
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .fillMaxHeight(0.9f)
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Send Feedback",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Crash alert if crash log present
                    if (crashLog != null) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
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
                                        "We found a recent crash. Please describe what you were doing when it occurred.",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                    
                    // Feedback Type Selection
                    Text(
                        "Feedback Type",
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
                                label = { Text(type.label.split(" ").last()) },
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
                                label = { Text(type.label.split(" ").last()) },
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
                                    FeedbackType.CRASH -> "What were you doing when the app crashed?"
                                    FeedbackType.GENERAL -> "Share your thoughts..."
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        maxLines = 8,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Default
                        )
                    )
                    
                    // Optional Email
                    OutlinedTextField(
                        value = userEmail,
                        onValueChange = { userEmail = it },
                        label = { Text("Your email (optional)") },
                        placeholder = { Text("For follow-up questions") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next
                        )
                    )
                    
                    // Include Options
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
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
                            }
                            
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
                        }
                    }
                    
                    // Send Method Selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = sendMethod == SendMethod.EMAIL_APP,
                            onClick = { sendMethod = SendMethod.EMAIL_APP },
                            label = { 
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("Email App")
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = sendMethod == SendMethod.COPY,
                            onClick = { sendMethod = SendMethod.COPY },
                            label = { 
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("Copy to Clipboard")
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val fullFeedback = buildFullFeedback(
                                feedbackType = feedbackType,
                                feedbackText = feedbackText,
                                userEmail = userEmail,
                                includeDeviceInfo = includeDeviceInfo,
                                deviceInfo = deviceInfo,
                                includeCrashLog = includeCrashLog,
                                crashLog = crashLog
                            )
                            
                            when (sendMethod) {
                                SendMethod.EMAIL_APP -> {
                                    sendViaEmailApp(
                                        context = context,
                                        email = feedbackEmail,
                                        subject = "${feedbackType.label} - App v${BuildConfig.VERSION_NAME}",
                                        body = fullFeedback
                                    )
                                }
                                SendMethod.COPY -> {
                                    copyToClipboard(
                                        context = context,
                                        text = fullFeedback,
                                        email = feedbackEmail
                                    )
                                }
                            }
                            
                            scope.launch {
                                showSuccess = true
                                delay(2000)
                                onDismiss()
                            }
                        },
                        enabled = feedbackText.isNotBlank()
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send Feedback")
                    }
                }
            }
        }
    }
}

private fun buildFullFeedback(
    feedbackType: FeedbackType,
    feedbackText: String,
    userEmail: String,
    includeDeviceInfo: Boolean,
    deviceInfo: String,
    includeCrashLog: Boolean,
    crashLog: String?
): String {
    return buildString {
        appendLine("Type: ${feedbackType.label}")
        appendLine()
        appendLine("Message:")
        appendLine(feedbackText)
        
        if (userEmail.isNotBlank()) {
            appendLine()
            appendLine("User email: $userEmail")
        }
        
        if (includeDeviceInfo) {
            append(deviceInfo)
        }
        
        if (includeCrashLog && crashLog != null) {
            appendLine()
            appendLine("---")
            appendLine("Crash Log:")
            appendLine(crashLog)
        }
    }
}

private fun sendViaEmailApp(
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
        copyToClipboard(context, body, email)
    }
}

private fun copyToClipboard(
    context: Context,
    text: String,
    email: String
) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Feedback", text)
    clipboard.setPrimaryClip(clip)
    
    Toast.makeText(
        context,
        "Feedback copied!\nSend to: $email",
        Toast.LENGTH_LONG
    ).show()
}

private fun getAvailableMemory(context: Context): String {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memoryInfo = ActivityManager.MemoryInfo()
    activityManager.getMemoryInfo(memoryInfo)
    val availableMemory = memoryInfo.availMem / (1024 * 1024)
    val totalMemory = memoryInfo.totalMem / (1024 * 1024)
    return "${availableMemory}MB / ${totalMemory}MB"
}

private fun getTotalStorage(): String {
    val stat = StatFs(Environment.getDataDirectory().path)
    val available = (stat.availableBlocksLong * stat.blockSizeLong) / (1024 * 1024 * 1024)
    val total = (stat.blockCountLong * stat.blockSizeLong) / (1024 * 1024 * 1024)
    return "${available}GB / ${total}GB"
}

/**
 * Crash Handler class to capture uncaught exceptions
 */
class CrashHandler(
    private val context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {
    
    companion object {
        private const val CRASH_LOG_KEY = "last_crash_log"
        private const val CRASH_TIME_KEY = "last_crash_time"
        private const val PREFS_NAME = "crash_logs"
        
        fun install(context: Context) {
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler(
                CrashHandler(context, defaultHandler)
            )
        }
        
        fun getLastCrashLog(context: Context): String? {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(CRASH_LOG_KEY, null)
        }
        
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
            
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(CRASH_LOG_KEY, crashLog)
                .putLong(CRASH_TIME_KEY, System.currentTimeMillis())
                .apply()
            
        } catch (e: Exception) {
            // Ignore errors while saving crash log
        }
        
        // Pass to default handler
        defaultHandler?.uncaughtException(thread, exception)
    }
}
