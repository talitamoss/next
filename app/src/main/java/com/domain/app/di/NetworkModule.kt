package com.domain.app.di

import android.content.Context
import com.domain.app.network.P2PNetworkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Network-related dependency injection module
 * Provides P2P networking components
 * 
 * File location: app/src/main/java/com/domain/app/di/NetworkModule.kt
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    fun provideP2PNetworkManager(
        @ApplicationContext context: Context
    ): P2PNetworkManager {
        return P2PNetworkManager(context)
    }
}
