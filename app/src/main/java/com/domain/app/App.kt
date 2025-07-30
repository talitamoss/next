package com.domain.app

import android.app.Application
import androidx.lifecycle.lifecycleScope
import com.domain.app.network.P2PNetworkManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Main Application class - initializes P2P service on startup
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
                Timber.d("Initializing P2P network...")
                
                // TODO: Get these from secure storage or generate on first run
                val nickname = getOrGenerateNickname()
                val password = getOrGeneratePassword()
                
                val result = p2pNetworkManager.initialize(nickname, password)
                
                result.fold(
                    onSuccess = { contactLink ->
                        Timber.d("P2P network initialized successfully")
                        Timber.d("Contact link: ${contactLink.take(50)}...")
                        
                        // Store contact link for sharing
                        saveContactLink(contactLink)
                    },
                    onFailure = { error ->
                        Timber.e(error, "Failed to initialize P2P network")
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
    
    private fun getOrGeneratePassword(): String {
        // In production, use Android Keystore for secure storage
        val prefs = getSharedPreferences("p2p_prefs", MODE_PRIVATE)
        return prefs.getString("password", null) ?: run {
            val generated = generateSecurePassword()
            prefs.edit().putString("password", generated).apply()
            generated
        }
    }
    
    private fun generateSecurePassword(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()"
        return (1..16)
            .map { chars.random() }
            .joinToString("")
    }
    
    private fun saveContactLink(link: String) {
        val prefs = getSharedPreferences("p2p_prefs", MODE_PRIVATE)
        prefs.edit().putString("contact_link", link).apply()
    }
    
    companion object {
        fun getStoredContactLink(app: Application): String? {
            val prefs = app.getSharedPreferences("p2p_prefs", MODE_PRIVATE)
            return prefs.getString("contact_link", null)
        }
    }
}
