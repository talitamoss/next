package com.domain.app.di

import android.content.Context
import com.domain.app.App
import com.domain.app.core.storage.AppDatabase
import com.domain.app.core.storage.dao.DataPointDao
import com.domain.app.core.storage.dao.PluginStateDao
import com.domain.app.core.storage.encryption.EncryptionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideEncryptionManager(@ApplicationContext context: Context): EncryptionManager {
        return (context as App).encryptionManager
    }
    
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        encryptionManager: EncryptionManager
    ): AppDatabase {
        val dbKey = encryptionManager.getDatabaseKey()
        return AppDatabase.getInstance(context, dbKey)
    }
    
    @Provides
    fun provideDataPointDao(database: AppDatabase): DataPointDao {
        return database.dataPointDao()
    }
    
    @Provides
    fun providePluginStateDao(database: AppDatabase): PluginStateDao {
        return database.pluginStateDao()
    }
}
