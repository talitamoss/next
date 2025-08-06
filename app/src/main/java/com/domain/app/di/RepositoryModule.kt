// app/src/main/java/com/domain/app/di/RepositoryModule.kt
package com.domain.app.di

import com.domain.app.core.data.DataRepository
import com.domain.app.core.export.ExportManager
import com.domain.app.core.plugin.PluginManager
import com.domain.app.core.storage.dao.DataPointDao
import com.domain.app.core.storage.encryption.EncryptionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    
    @Provides
    @Singleton
    fun provideDataRepository(
        dataPointDao: DataPointDao,
        encryptionManager: EncryptionManager
    ): DataRepository {
        return DataRepository(dataPointDao, encryptionManager)
    }
    
    @Provides
    @Singleton
    fun provideExportManager(
        dataRepository: DataRepository,
        pluginManager: PluginManager
    ): ExportManager {
        return ExportManager(dataRepository, pluginManager)
    }
}
