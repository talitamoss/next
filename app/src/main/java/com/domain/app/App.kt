// app/src/main/java/com/domain/app/App.kt
package com.domain.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.domain.app.core.storage.encryption.EncryptionManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application class for app-wide initialization
 */
@HiltAndroidApp
class App : Application(), Configuration.Provider {
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    // ADD THIS PROPERTY - This is what DatabaseModule needs!
    lateinit var encryptionManager: EncryptionManager
        private set
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize encryption manager BEFORE other components
        encryptionManager = EncryptionManager(this)
        
        // Initialize any app-wide components here
        initializeApp()
    }
    
    /**
     * Provide WorkManager configuration with Hilt integration
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    
    private fun initializeApp() {
        // Initialize crash reporting, analytics, etc.
        // This is where you'd initialize any third-party SDKs
    }
    
    companion object {
        lateinit var instance: App
            private set
    }
    
    init {
        instance = this
    }
}
