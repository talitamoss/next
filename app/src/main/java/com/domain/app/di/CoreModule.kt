package com.domain.app.di

import com.domain.app.core.EventBus
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Core components dependency injection module
 * Provides singleton instances of core app components
 * 
 * File location: app/src/main/java/com/domain/app/di/CoreModule.kt
 */
@Module
@InstallIn(SingletonComponent::class)
object CoreModule {
    
    @Provides
    @Singleton
    fun provideEventBus(): EventBus {
        return EventBus
    }
}
