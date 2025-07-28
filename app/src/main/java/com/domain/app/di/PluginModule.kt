package com.domain.app.di

import android.content.Context
import com.domain.app.core.data.DataRepository
import com.domain.app.core.plugin.PluginManager
import com.domain.app.core.plugin.PluginRegistry
import com.domain.app.core.plugin.security.PluginPermissionManager
import com.domain.app.core.plugin.security.SecurityMonitor
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
        dataRepository: DataRepository,
        pluginRegistry: PluginRegistry,
        permissionManager: PluginPermissionManager,
        securityMonitor: SecurityMonitor
    ): PluginManager {
        return PluginManager(
            context = context,
            database = database,
            dataRepository = dataRepository,
            pluginRegistry = pluginRegistry,
            permissionManager = permissionManager,
            securityMonitor = securityMonitor
        )
    }
}
