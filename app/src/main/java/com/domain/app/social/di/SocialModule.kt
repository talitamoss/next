package com.domain.app.social.di

import android.content.Context
import com.domain.app.social.contracts.MockSocialRepository
import com.domain.app.social.contracts.SocialRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for social features
 * Switches between mock and real implementations based on feature flag
 */
@Module
@InstallIn(SingletonComponent::class)
object SocialModule {
    
    /**
     * Feature flag to control which implementation to use
     * Set to true for UI development with mocks
     * Set to false for real BitChat implementation
     */
    private const val USE_MOCK_SOCIAL = true
    
    @Provides
    @Singleton
    fun provideSocialRepository(
        @ApplicationContext context: Context,
        mockRepository: MockSocialRepository
        // TODO: Add real implementation parameters when ready
        // bitChatService: BitChatService,
        // networkManager: P2PNetworkManager,
        // dataRepository: DataRepository,
        // encryptionManager: EncryptionManager
    ): SocialRepository {
        return if (USE_MOCK_SOCIAL) {
            mockRepository
        } else {
            // TODO: Return real implementation when backend is ready
            // BitChatSocialRepository(bitChatService, networkManager, dataRepository, encryptionManager)
            throw NotImplementedError("Real social repository not yet implemented. Set USE_MOCK_SOCIAL = true")
        }
    }
}
