// app/src/main/java/com/domain/app/di/BackupModule.kt
package com.domain.app.di

import android.content.Context
import com.domain.app.core.backup.BackupManager
import com.domain.app.core.data.DataRepository
import com.domain.app.core.storage.encryption.EncryptionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BackupModule {
    
    @Provides
    @Singleton
    fun provideBackupManager(
        @ApplicationContext context: Context,
        dataRepository: DataRepository,
        encryptionManager: EncryptionManager
    ): BackupManager {
        return BackupManager(context, dataRepository, encryptionManager)
    }
}
