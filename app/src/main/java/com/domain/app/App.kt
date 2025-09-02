// app/src/main/java/com/domain/app/App.kt
package com.domain.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application class for app-wide initialization
 */
@HiltAndroidApp
class App : Application(), Configuration.Provider {
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    override fun onCreate() {
        super.onCreate()
        
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
}

// Don't forget to add this to AndroidManifest.xml:
// <application
//     android:name=".App"
//     ... >
