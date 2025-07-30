package com.domain.app.network.di

import android.content.Context
import com.domain.app.core.data.DataRepository
import com.domain.app.network.*
import com.domain.app.network.protocol.P2PProtocolHandler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

/**
 * Dependency injection module for network components
 * Provides BitChat P2P networking dependencies
 * 
 * File location: app/src/main/java/com/domain/app/network/di/NetworkModule.kt
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    fun provideBitChatService(
        @ApplicationContext context: Context
    ): BitChatService {
        return BitChatService(context)
    }
    
    @Provides
    @Singleton
    fun provideP2PProtocolHandler(): P2PProtocolHandler {
        return P2PProtocolHandler()
    }
    
    @Provides
    @Singleton
    fun provideContentStore(): ContentStore {
        return ContentStore()
    }
    
    @Provides
    @Singleton
    fun provideNetworkCoroutineScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
    
    @Provides
    @Singleton
    fun provideP2PNetworkManager(
        bitChatService: BitChatService,
        protocolHandler: P2PProtocolHandler,
        dataRepository: DataRepository,
        contentStore: ContentStore,
        coroutineScope: CoroutineScope
    ): P2PNetworkManager {
        return P2PNetworkManager(
            bitChatService = bitChatService,
            protocol = protocolHandler,
            dataRepository = dataRepository,
            contentStore = contentStore,
            coroutineScope = coroutineScope
        )
    }
}
