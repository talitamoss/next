package com.domain.app.network

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.domain.app.MainActivity
import com.domain.app.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import timber.log.Timber
import javax.inject.Inject

/**
 * Foreground service to keep BitChat P2P connection alive
 * Manages Bluetooth LE scanning and advertising in the background
 * 
 * File location: app/src/main/java/com/domain/app/network/P2PForegroundService.kt
 */
@AndroidEntryPoint
class P2PForegroundService : Service() {
    
    @Inject
    lateinit var bitChatService: BitChatService
    
    @Inject
    lateinit var networkManager: P2PNetworkManager
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "bitchat_service_channel"
        const val ACTION_STOP = "com.domain.app.action.STOP_BITCHAT"
        const val ACTION_SYNC = "com.domain.app.action.SYNC_BITCHAT"
        const val EXTRA_NICKNAME = "nickname"
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Timber.d("P2P Foreground Service created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopService()
                return START_NOT_STICKY
            }
            ACTION_SYNC -> {
                syncWithPeers()
                return START_STICKY
            }
        }
        
        // Initialize BitChat if not already running
        if (bitChatService.connectionState.value != ConnectionState.READY) {
            val nickname = intent?.getStringExtra(EXTRA_NICKNAME) ?: "User"
            initializeBitChat(nickname)
        }
        
        // Create and display notification
        val notification = createNotification()
        
        // Start foreground service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID, 
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        Timber.d("P2P Foreground Service destroyed")
        
        // Clean up
        serviceScope.cancel()
        bitChatService.stop()
    }
    
    /**
     * Initialize BitChat service
     */
    private fun initializeBitChat(nickname: String) {
        serviceScope.launch {
            try {
                // Initialize BitChat
                val result = bitChatService.initialize()
                
                if (result.isSuccess) {
                    // Initialize network manager
                    networkManager.initialize(nickname)
                    
                    // Start automatic sync
                    startAutomaticSync()
                    
                    Timber.d("BitChat initialized successfully")
                } else {
                    Timber.e("Failed to initialize BitChat: ${result.exceptionOrNull()}")
                    stopSelf()
                }
            } catch (e: Exception) {
                Timber.e(e, "Error initializing BitChat")
                stopSelf()
            }
        }
    }
    
    /**
     * Start automatic peer synchronization
     */
    private fun startAutomaticSync() {
        serviceScope.launch {
            while (isActive) {
                delay(60000) // Sync every minute
                
                if (bitChatService.connectedPeers.value.isNotEmpty()) {
                    networkManager.pullFeedFromPeers()
                }
            }
        }
    }
    
    /**
     * Manually trigger sync with peers
     */
    private fun syncWithPeers() {
        serviceScope.launch {
            try {
                networkManager.pullFeedFromPeers()
                Timber.d("Manual sync completed")
            } catch (e: Exception) {
                Timber.e(e, "Manual sync failed")
            }
        }
    }
    
    /**
     * Stop the service gracefully
     */
    private fun stopService() {
        serviceScope.cancel()
        bitChatService.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    
    /**
     * Create notification channel for Android O+
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "BitChat P2P Connection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Maintains peer-to-peer connections via Bluetooth"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Create notification for foreground service
     */
    private fun createNotification(): Notification {
        // Intent to open app
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 
            0, 
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Intent to stop service
        val stopIntent = Intent(this, P2PForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Intent to manually sync
        val syncIntent = Intent(this, P2PForegroundService::class.java).apply {
            action = ACTION_SYNC
        }
        val syncPendingIntent = PendingIntent.getService(
            this,
            2,
            syncIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Get connection status
        val connectionStatus = when (bitChatService.connectionState.value) {
            ConnectionState.READY -> {
                val peerCount = bitChatService.connectedPeers.value.size
                if (peerCount > 0) {
                    "$peerCount peers connected"
                } else {
                    "Searching for peers..."
                }
            }
            ConnectionState.STARTING -> "Starting BitChat..."
            ConnectionState.ERROR -> "Connection error"
            ConnectionState.NOT_STARTED -> "Not started"
        }
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BitChat P2P Active")
            .setContentText(connectionStatus)
            .setSmallIcon(android.R.drawable.stat_notify_sync) // Using system icon
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_popup_sync,
                "Sync",
                syncPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop",
                stopPendingIntent
            )
            .build()
    }
}
