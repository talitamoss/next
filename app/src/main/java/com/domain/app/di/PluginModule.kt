package com.domain.app.di

import android.content.Context
import com.domain.app.core.plugin.PluginManager
import com.domain.app.core.plugin.PluginRegistry
import com.domain.app.core.storage.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PluginModule {
    
    @Provides
    @Singleton
    fun providePluginRegistry(): PluginRegistry {
        return PluginRegistry()
    }
    
    @Provides
    @Singleton
    fun providePluginManager(
        @ApplicationContext context: Context,
        database: AppDatabase,
        pluginRegistry: PluginRegistry
    ): PluginManager {
        return PluginManager(context, database, pluginRegistry)
    }
}
