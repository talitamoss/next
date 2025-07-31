package com.domain.app.di

import android.content.Context
import com.domain.app.core.plugin.security.*
import com.domain.app.core.storage.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Security-related dependency injection module
 */
@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {
    
    @Provides
    @Singleton
    fun provideSecurityAuditLogger(): SecurityAuditLogger {
        return SecurityAuditLogger()
    }
    
    @Provides
    @Singleton
    fun providePluginPermissionManager(
        @ApplicationContext context: Context
    ): PluginPermissionManager {
        return PluginPermissionManager(context)
    }
    
    @Provides
    @Singleton
    fun provideDataEncryption(): DataEncryption {
        return DataEncryption()
    }
}
