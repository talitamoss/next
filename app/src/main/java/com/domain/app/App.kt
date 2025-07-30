package com.domain.app

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.domain.app.network.P2PNetworkManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Main Application class - initializes BitChat P2P service on startup
 * 
 * File location: app/src/main/java/com/domain/app/App.kt
 */
@HiltAndroidApp
class App : Application() {
    
    @Inject
    lateinit var p2pNetworkManager: P2PNetworkManager
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        
        // Initialize P2P network in background
        initializeP2PNetwork()
    }
    
    private fun initializeP2PNetwork() {
        // Use process lifecycle scope
        ProcessLifecycleOwner.get().lifecycleScope.launch {
            try {
                Timber.d("Initializing BitChat P2P network...")
                
                // TODO: Get these from secure storage or generate on first run
                val nickname = getOrGenerateNickname()
                
                val result = p2pNetworkManager.initialize(nickname)
                
                result.fold(
                    onSuccess = {
                        Timber.d("BitChat P2P network initialized successfully")
                        Timber.d("Local peer ID: $nickname")
                        
                        // Store initialization state
                        saveInitializationState(true)
                    },
                    onFailure = { error ->
                        Timber.e(error, "Failed to initialize BitChat P2P network")
                        // Handle initialization failure
                        // Maybe retry or show notification
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Unexpected error during P2P initialization")
            }
        }
    }
    
    private fun getOrGenerateNickname(): String {
        val prefs = getSharedPreferences("p2p_prefs", MODE_PRIVATE)
        return prefs.getString("nickname", null) ?: run {
            val generated = "User${(1000..9999).random()}"
            prefs.edit().putString("nickname", generated).apply()
            generated
        }
    }
    
    private fun saveInitializationState(initialized: Boolean) {
        val prefs = getSharedPreferences("p2p_prefs", MODE_PRIVATE)
        prefs.edit().putBoolean("p2p_initialized", initialized).apply()
    }
}
