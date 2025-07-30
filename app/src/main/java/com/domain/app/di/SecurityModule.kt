package com.domain.app.di

import android.content.Context
import com.domain.app.core.plugin.security.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger module for security-related dependencies
 * 
 * File location: app/src/main/java/com/domain/app/di/SecurityModule.kt
 */
@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {
    
    @Provides
    @Singleton
    fun provideSecurityAuditLogger(
        @ApplicationContext context: Context
    ): SecurityAuditLogger {
        return SecurityAuditLogger(context)
    }
    
    @Provides
    @Singleton
    fun providePluginPermissionManager(
        @ApplicationContext context: Context
    ): PluginPermissionManager {
        return PluginPermissionManager(context)
    }
}
