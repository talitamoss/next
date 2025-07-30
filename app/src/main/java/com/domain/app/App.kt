package com.domain.app

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.HiltAndroidApp

// Extension property to get the shared contact link DataStore
val Context.contactLinkDataStore: DataStore<Preferences> by preferencesDataStore(name = "contact_link")

@HiltAndroidApp
class App : Application() {
    
    companion object {
        lateinit var instance: App
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
    }
    
    /**
     * Get the stored contact link from preferences
     */
    suspend fun getStoredContactLink(): String? {
        // This would be implemented using the DataStore
        // For now, return a placeholder
        return null
    }
}
