// app/src/main/java/com/domain/app/di/BackupModule.kt
package com.domain.app.di

import android.content.Context
import com.domain.app.core.backup.BackupManager
import com.domain.app.core.data.DataRepository
import com.domain.app.core.encryption.EncryptionManager
import com.domain.app.core.plugin.PluginManager
import com.domain.app.core.preferences.UserPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for backup-related components
 */
@Module
@InstallIn(SingletonComponent::class)
object BackupModule {
    
    /**
     * Provides singleton instance of BackupManager
     */
    @Provides
    @Singleton
    fun provideBackupManager(
        @ApplicationContext context: Context,
        dataRepository: DataRepository,
        encryptionManager: EncryptionManager,
        pluginManager: PluginManager,
        userPreferences: UserPreferences
    ): BackupManager {
        return BackupManager(
            context = context,
            dataRepository = dataRepository,
            encryptionManager = encryptionManager,
            pluginManager = pluginManager,
            userPreferences = userPreferences
        )
    }
}
