package com.domain.app.social.di

import com.domain.app.social.contracts.*
import com.domain.app.social.data.MockSocialFeedRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SocialModule {

    @Provides
    @Singleton
    fun provideSocialRepository(): SocialRepository {
        val mockContacts = listOf(
            SocialContact(
                id = "1",
                displayName = "Cashka",
                isOnline = true,
                trustLevel = TrustLevel.CLOSE_FRIEND
            ),
            SocialContact(
                id = "2",
                displayName = "Jordan",
                isOnline = false,
                trustLevel = TrustLevel.FRIEND
            ),
            SocialContact(
                id = "3",
                displayName = "Sam",
                isOnline = true,
                trustLevel = TrustLevel.FAMILY
            )
        )
        return MockSocialRepository(mockContacts)
    }

    @Provides
    @Singleton
    fun provideSocialFeedRepository(): SocialFeedRepository {
        return MockSocialFeedRepository()
    }
}
