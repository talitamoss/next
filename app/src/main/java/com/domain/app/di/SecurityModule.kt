package com.domain.app.di

import android.content.Context
import com.domain.app.core.plugin.security.PluginPermissionManager
import com.domain.app.core.plugin.security.SecurityMonitor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {
    
    @Provides
    @Singleton
    fun provideSecurityMonitor(): SecurityMonitor {
        return SecurityMonitor()
    }
    
    @Provides
    @Singleton
    fun providePluginPermissionManager(
        @ApplicationContext context: Context,
        securityMonitor: SecurityMonitor
    ): PluginPermissionManager {
        return PluginPermissionManager(context, securityMonitor)
    }
}
