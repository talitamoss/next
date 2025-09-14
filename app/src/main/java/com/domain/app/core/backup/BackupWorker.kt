// app/src/main/java/com/domain/app/core/backup/BackupWorker.kt
package com.domain.app.core.backup

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Simplified BackupWorker for scheduled backups
 * TODO: Implement full functionality later
 */
@HiltWorker
class BackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val backupManager: BackupManager
) : CoroutineWorker(context, workerParams) {
    
    override suspend fun doWork(): Result {
        return try {
            // Create a simple backup
            val backupResult = backupManager.createBackup(
                location = null,
                encrypt = true
            )
            
            when (backupResult) {
                is BackupResult.Success -> Result.success()
                is BackupResult.Error -> Result.failure()
            }
        } catch (e: Exception) {
            Result.failure()
        }
    }
    
    companion object {
        private const val BACKUP_WORK_NAME = "periodic_backup"
        
        fun schedule(
            context: Context,
            frequency: BackupFrequency = BackupFrequency.DAILY,
            requiresWifi: Boolean = true,
            requiresCharging: Boolean = false
        ) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(
                    if (requiresWifi) NetworkType.UNMETERED else NetworkType.NOT_REQUIRED
                )
                .setRequiresCharging(requiresCharging)
                .build()
            
            val backupRequest = PeriodicWorkRequestBuilder<BackupWorker>(
                frequency.interval,
                frequency.timeUnit
            )
                .setConstraints(constraints)
                .build()
            
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    BACKUP_WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    backupRequest
                )
        }
        
        fun cancel(context: Context) {
            WorkManager.getInstance(context)
                .cancelUniqueWork(BACKUP_WORK_NAME)
        }
    }
}

enum class BackupFrequency(val interval: Long, val timeUnit: TimeUnit) {
    DAILY(1, TimeUnit.DAYS),
    WEEKLY(7, TimeUnit.DAYS),
    MONTHLY(30, TimeUnit.DAYS)
}

// Extension functions for easy access
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

fun Context.cancelAutoBackup() {
    BackupWorker.cancel(this)
}
