package com.domain.app.di

import android.content.Context
import com.domain.app.core.storage.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Database dependency injection module
 * 
 * File location: app/src/main/java/com/domain/app/di/DatabaseModule.kt
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return AppDatabase.getInstance(context)
    }
    
    @Provides
    fun provideDataPointDao(database: AppDatabase) = database.dataPointDao()
    
    @Provides
    fun providePluginStateDao(database: AppDatabase) = database.pluginStateDao()
}

