package com.domain.app.social.contracts

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.Instant
import java.time.temporal.ChronoUnit

class MockSocialRepository(
    private val mockContacts: List<SocialContact> = emptyList(),
    private val mockFeedItems: List<FeedItem> = listOf(
        FeedItem(
            id = "1",
            authorId = "1",
            authorName = "Cashka",
            contentType = ContentType.DATA_INSIGHT,
            content = FeedContent.DataInsight("Hit my step goal!", "12,847", "steps", "🚶‍♂️"),
            timestamp = Instant.now().minus(2, ChronoUnit.HOURS),
            isRead = false,
            canReshare = false,
            expiresAt = Instant.now().plus(2, ChronoUnit.DAYS)
        )
    )
) : SocialRepository {
    override fun getContacts(): Flow<List<SocialContact>> = flowOf(mockContacts)

    override suspend fun addContact(contactCode: String): Result<SocialContact> =
        Result.success(mockContacts.first())

    override suspend fun removeContact(contactId: String): Result<Unit> = Result.success(Unit)

    override fun getFeed(): Flow<List<FeedItem>> = flowOf(mockFeedItems)

    override suspend fun refreshFeed(): Result<Unit> = Result.success(Unit)

    override suspend fun markAsRead(itemId: String) {}

    override suspend fun shareContent(dataPointId: String, permissions: SharingPermissions): Result<Unit> =
        Result.success(Unit)

    override fun getShareableContent(): Flow<List<ShareableDataPoint>> = flowOf(emptyList())
}
