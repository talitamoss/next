package com.domain.app

import android.app.Application
import com.domain.app.core.storage.encryption.EncryptionManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App : Application() {
    
    lateinit var encryptionManager: EncryptionManager
        private set
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize encryption manager
        encryptionManager = EncryptionManager(this)
        
        // Initialize other app-wide components here
        initializeLogging()
    }
    
    private fun initializeLogging() {
        // In production, initialize crash reporting and logging
    }
    
    companion object {
        lateinit var instance: App
            private set
    }
    
    init {
        instance = this
    }
}
