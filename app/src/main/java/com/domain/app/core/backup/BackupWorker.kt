// app/src/main/java/com/domain/app/core/backup/BackupWorker.kt
package com.domain.app.core.backup

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.domain.app.R
import com.domain.app.core.preferences.UserPreferences
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Worker class that performs automatic backups in the background
 */
@HiltWorker
class BackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val backupManager: BackupManager,
    private val userPreferences: UserPreferences
) : CoroutineWorker(context, params) {
    
    companion object {
        const val WORK_NAME = "auto_backup_work"
        const val NOTIFICATION_CHANNEL_ID = "backup_channel"
        const val NOTIFICATION_ID = 1001
        
        /**
         * Schedule periodic backup work
         */
        fun schedule(
            context: Context,
            frequency: BackupFrequency = BackupFrequency.DAILY,
            requiresWifi: Boolean = true,
            requiresCharging: Boolean = false
        ) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(
                    if (requiresWifi) NetworkType.UNMETERED else NetworkType.CONNECTED
                )
                .setRequiresCharging(requiresCharging)
                .setRequiresStorageNotLow(true)
                .build()
            
            val backupRequest = PeriodicWorkRequestBuilder<BackupWorker>(
                frequency.repeatInterval,
                frequency.timeUnit
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .addTag("backup")
                .build()
            
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    backupRequest
                )
        }
        
        /**
         * Cancel scheduled backup work
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context)
                .cancelUniqueWork(WORK_NAME)
        }
        
        /**
         * Schedule a one-time backup
         */
        fun scheduleOneTime(
            context: Context,
            delayMinutes: Long = 0
        ) {
            val constraints = Constraints.Builder()
                .setRequiresStorageNotLow(true)
                .build()
            
            val backupRequest = OneTimeWorkRequestBuilder<BackupWorker>()
                .setConstraints(constraints)
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .addTag("backup_one_time")
                .build()
            
            WorkManager.getInstance(context)
                .enqueue(backupRequest)
        }
    }
    
    override suspend fun doWork(): Result {
        // Check if auto-backup is still enabled
        val autoBackupEnabled = userPreferences.autoBackupEnabled.first()
        if (!autoBackupEnabled) {
            return Result.success()
        }
        
        // Show notification that backup is in progress
        showProgressNotification()
        
        return try {
            // Perform backup
            val result = backupManager.createBackup(
                format = BackupFormat.JSON,
                includePluginData = true,
                encrypt = true
            )
            
            when (result) {
                is BackupResult.Success -> {
                    // Clean up old backups
                    backupManager.cleanupOldBackups(keepCount = 7)
                    
                    // Show success notification
                    showSuccessNotification(result)
                    
                    // Return success with output data
                    Result.success(
                        workDataOf(
                            "backup_file" to result.backupFile.absolutePath,
                            "item_count" to result.itemCount,
                            "size_bytes" to result.sizeBytes,
                            "timestamp" to result.timestamp
                        )
                    )
                }
                is BackupResult.Failure -> {
                    // Show error notification
                    showErrorNotification(result.error)
                    
                    // Check retry count
                    if (runAttemptCount < 3) {
                        Result.retry()
                    } else {
                        Result.failure(
                            workDataOf("error" to result.error)
                        )
                    }
                }
            }
        } catch (e: Exception) {
            showErrorNotification(e.message ?: "Unknown error")
            
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure(
                    workDataOf("error" to e.message)
                )
            }
        }
    }
    
    private fun showProgressNotification() {
        createNotificationChannel()
        
        val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Creating Backup")
            .setContentText("Backing up your data...")
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setProgress(0, 0, true)
            .build()
        
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) 
            as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun showSuccessNotification(result: BackupResult.Success) {
        val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Backup Complete")
            .setContentText("Backed up ${result.itemCount} items")
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) 
            as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun showErrorNotification(error: String) {
        val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Backup Failed")
            .setContentText(error)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) 
            as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Backup Notifications",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for automatic backup operations"
                setShowBadge(false)
            }
            
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) 
                as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}

/**
 * Backup frequency options
 */
enum class BackupFrequency(
    val repeatInterval: Long,
    val timeUnit: TimeUnit
) {
    DAILY(1, TimeUnit.DAYS),
    WEEKLY(7, TimeUnit.DAYS),
    MONTHLY(30, TimeUnit.DAYS)
}

/**
 * Extension function to schedule backups from DataManagementViewModel
 */
fun Context.scheduleAutoBackup(
    frequency: String,
    wifiOnly: Boolean = true
) {
    val backupFrequency = when (frequency.lowercase()) {
        "daily" -> BackupFrequency.DAILY
        "weekly" -> BackupFrequency.WEEKLY
        "monthly" -> BackupFrequency.MONTHLY
        else -> BackupFrequency.DAILY
    }
    
    BackupWorker.schedule(
        context = this,
        frequency = backupFrequency,
        requiresWifi = wifiOnly,
        requiresCharging = false
    )
}

/**
 * Extension function to cancel auto backups
 */
fun Context.cancelAutoBackup() {
    BackupWorker.cancel(this)
}
