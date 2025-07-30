package com.domain.app.social.contracts

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mock implementation of SocialRepository for UI development
 * Provides realistic fake data for all social features
 */
@Singleton
class MockSocialRepository @Inject constructor() : SocialRepository {
    
    private val _contacts = MutableStateFlow(generateMockContacts())
    private val _feedItems = MutableStateFlow(generateMockFeedItems())
    private val _contactRequests = MutableStateFlow(generateMockContactRequests())
    private val _shareableContent = MutableStateFlow(generateMockShareableContent())
    private val _sharedContent = MutableStateFlow(generateMockSharedContent())
    private val _privacySettings = MutableStateFlow(PrivacySettings())
    private val _unreadCount = MutableStateFlow(3)
    
    // Contacts Management
    override fun getContacts(): Flow<List<SocialContact>> = _contacts.asStateFlow()
    
    override suspend fun addContact(contactCode: String): Result<SocialContact> {
        delay(1000) // Simulate network delay
        val newContact = SocialContact(
            id = "new_${System.currentTimeMillis()}",
            displayName = "New Friend",
            isOnline = true,
            trustLevel = TrustLevel.FRIEND,
            connectionQuality = ConnectionQuality.GOOD
        )
        _contacts.value = _contacts.value + newContact
        return Result.success(newContact)
    }
    
    override suspend fun removeContact(contactId: String): Result<Unit> {
        delay(500)
        _contacts.value = _contacts.value.filter { it.id != contactId }
        return Result.success(Unit)
    }
    
    override suspend fun blockContact(contactId: String): Result<Unit> {
        delay(500)
        _contacts.value = _contacts.value.map { contact ->
            if (contact.id == contactId) {
                contact.copy(trustLevel = TrustLevel.BLOCKED)
            } else contact
        }
        return Result.success(Unit)
    }
    
    override suspend fun updateContactTrustLevel(contactId: String, trustLevel: TrustLevel): Result<Unit> {
        delay(300)
        _contacts.value = _contacts.value.map { contact ->
            if (contact.id == contactId) {
                contact.copy(trustLevel = trustLevel)
            } else contact
        }
        return Result.success(Unit)
    }
    
    override fun getContactRequests(): Flow<List<ContactRequest>> = _contactRequests.asStateFlow()
    
    override suspend fun acceptContactRequest(requestId: String): Result<Unit> {
        delay(800)
        val request = _contactRequests.value.find { it.id == requestId }
        if (request != null) {
            // Add as contact
            val newContact = SocialContact(
                id = request.fromContactId,
                displayName = request.fromDisplayName,
                isOnline = false,
                trustLevel = TrustLevel.FRIEND
            )
            _contacts.value = _contacts.value + newContact
            
            // Remove request
            _contactRequests.value = _contactRequests.value.filter { it.id != requestId }
        }
        return Result.success(Unit)
    }
    
    override suspend fun declineContactRequest(requestId: String): Result<Unit> {
        delay(500)
        _contactRequests.value = _contactRequests.value.filter { it.id != requestId }
        return Result.success(Unit)
    }
    
    // Feed Management
    override fun getFeed(): Flow<List<FeedItem>> = _feedItems.asStateFlow()
    
    override suspend fun refreshFeed(): Result<Unit> {
        delay(1500) // Simulate network refresh
        // Add a new item to simulate fresh content
        val newItem = FeedItem(
            id = "refresh_${System.currentTimeMillis()}",
            authorId = "contact1",
            authorName = "Cashka",
            contentType = ContentType.DATA_INSIGHT,
            content = FeedContent.DataInsight(
                title = "Just refreshed the feed!",
                description = "Testing the refresh functionality",
                value = "100",
                unit = "%",
                emoji = "🔄",
                pluginId = "refresh"
            ),
            timestamp = Instant.now(),
            privacyLevel = PrivacyLevel.FRIENDS,
            expiresAt = Instant.now().plus(1, ChronoUnit.DAYS)
        )
        _feedItems.value = listOf(newItem) + _feedItems.value
        return Result.success(Unit)
    }
    
    override suspend fun markAsRead(itemId: String) {
        _feedItems.value = _feedItems.value.map { item ->
            if (item.id == itemId) item.copy(isRead = true) else item
        }
        updateUnreadCount()
    }
    
    override suspend fun markAllAsRead(): Result<Unit> {
        _feedItems.value = _feedItems.value.map { it.copy(isRead = true) }
        _unreadCount.value = 0
        return Result.success(Unit)
    }
    
    override fun getUnreadCount(): Flow<Int> = _unreadCount.asStateFlow()
    
    // Content Sharing
    override suspend fun shareContent(dataPointId: String, permissions: SharingPermissions): Result<Unit> {
        delay(1000)
        val shareableItem = _shareableContent.value.find { it.id == dataPointId }
        if (shareableItem != null) {
            val sharedItem = FeedItem(
                id = "shared_${System.currentTimeMillis()}",
                authorId = "me",
                authorName = "You",
                contentType = shareableItem.contentType,
                content = FeedContent.DataInsight(
                    title = shareableItem.title,
                    description = shareableItem.preview,
                    value = "Shared",
                    emoji = "📤",
                    pluginId = shareableItem.pluginId
                ),
                timestamp = Instant.now(),
                privacyLevel = permissions.privacyLevel,
                expiresAt = permissions.expiresAt
            )
            _sharedContent.value = listOf(sharedItem) + _sharedContent.value
        }
        return Result.success(Unit)
    }
    
    override fun getShareableContent(): Flow<List<ShareableDataPoint>> = _shareableContent.asStateFlow()
    
    override suspend fun deleteSharedContent(shareId: String): Result<Unit> {
        delay(500)
        _sharedContent.value = _sharedContent.value.filter { it.id != shareId }
        return Result.success(Unit)
    }
    
    override fun getSharedContent(): Flow<List<FeedItem>> = _sharedContent.asStateFlow()
    
    // Reactions & Interactions
    override suspend fun addReaction(itemId: String, emoji: String): Result<Unit> {
        delay(300)
        _feedItems.value = _feedItems.value.map { item ->
            if (item.id == itemId) {
                val newReaction = Reaction(
                    userId = "me",
                    userName = "You",
                    emoji = emoji,
                    timestamp = Instant.now()
                )
                item.copy(reactions = item.reactions + newReaction)
            } else item
        }
        return Result.success(Unit)
    }
    
    override suspend fun removeReaction(itemId: String): Result<Unit> {
        delay(300)
        _feedItems.value = _feedItems.value.map { item ->
            if (item.id == itemId) {
                item.copy(reactions = item.reactions.filter { it.userId != "me" })
            } else item
        }
        return Result.success(Unit)
    }
    
    override suspend fun reshareContent(itemId: String, permissions: SharingPermissions): Result<Unit> {
        delay(800)
        val originalItem = _feedItems.value.find { it.id == itemId }
        if (originalItem != null && originalItem.canReshare) {
            val resharedItem = originalItem.copy(
                id = "reshare_${System.currentTimeMillis()}",
                authorId = "me",
                authorName = "You (reshared)",
                timestamp = Instant.now(),
                privacyLevel = permissions.privacyLevel
            )
            _sharedContent.value = listOf(resharedItem) + _sharedContent.value
        }
        return Result.success(Unit)
    }
    
    // Statistics & Analytics
    override fun getSocialStats(): Flow<SocialStats> {
        return _contacts.map { contacts ->
            SocialStats(
                totalFriends = contacts.size,
                onlineFriends = contacts.count { it.isOnline },
                unreadItems = _unreadCount.value,
                itemsSharedToday = 2,
                itemsReceivedToday = 5
            )
        }
    }
    
    override suspend fun getContactActivity(contactId: String): Result<List<FeedItem>> {
        delay(800)
        val contactItems = _feedItems.value.filter { it.authorId == contactId }
        return Result.success(contactItems)
    }
    
    // Privacy & Security
    override suspend fun updatePrivacySettings(settings: PrivacySettings): Result<Unit> {
        delay(500)
        _privacySettings.value = settings
        return Result.success(Unit)
    }
    
    override fun getPrivacySettings(): Flow<PrivacySettings> = _privacySettings.asStateFlow()
    
    override suspend fun reportContent(itemId: String, reason: String): Result<Unit> {
        delay(1000)
        // In real implementation, this would report to moderators
        return Result.success(Unit)
    }
    
    private fun updateUnreadCount() {
        _unreadCount.value = _feedItems.value.count { !it.isRead }
    }
    
    // ===== MOCK DATA GENERATORS =====
    
    private fun generateMockContacts() = listOf(
        SocialContact(
            id = "contact1",
            displayName = "Cashka",
            isOnline = true,
            trustLevel = TrustLevel.CLOSE_FRIEND,
            connectionQuality = ConnectionQuality.EXCELLENT,
            lastSeen = Instant.now().minus(5, ChronoUnit.MINUTES)
        ),
        SocialContact(
            id = "contact2",
            displayName = "Jordan",
            isOnline = false,
            trustLevel = TrustLevel.FRIEND,
            connectionQuality = ConnectionQuality.GOOD,
            lastSeen = Instant.now().minus(2, ChronoUnit.HOURS)
        ),
        SocialContact(
            id = "contact3",
            displayName = "Sam",
            isOnline = true,
            trustLevel = TrustLevel.FAMILY,
            connectionQuality = ConnectionQuality.EXCELLENT,
            lastSeen = Instant.now().minus(1, ChronoUnit.MINUTES)
        ),
        SocialContact(
            id = "contact4",
            displayName = "Alex",
            isOnline = false,
            trustLevel = TrustLevel.FRIEND,
            connectionQuality = ConnectionQuality.POOR,
            lastSeen = Instant.now().minus(1, ChronoUnit.DAYS)
        ),
        SocialContact(
            id = "contact5",
            displayName = "Morgan",
            isOnline = true,
            trustLevel = TrustLevel.ACQUAINTANCE,
            connectionQuality = ConnectionQuality.GOOD,
            lastSeen = Instant.now().minus(30, ChronoUnit.MINUTES)
        )
    )
    
    private fun generateMockFeedItems() = listOf(
        FeedItem(
            id = "feed1",
            authorId = "contact1",
            authorName = "Cashka",
            contentType = ContentType.DATA_INSIGHT,
            content = FeedContent.DataInsight(
                title = "Hit my step goal! 🎉",
                description = "Feeling great about staying active today",
                value = "12,847",
                unit = "steps",
                emoji = "🚶‍♂️",
                pluginId = "steps"
            ),
            timestamp = Instant.now().minus(2, ChronoUnit.HOURS),
            isRead = false,
            canReshare = true,
            privacyLevel = PrivacyLevel.FRIENDS,
            expiresAt = Instant.now().plus(2, ChronoUnit.DAYS),
            reactions = listOf(
                Reaction("contact2", "Jordan", "🔥", Instant.now().minus(1, ChronoUnit.HOURS)),
                Reaction("contact3", "Sam", "💪", Instant.now().minus(30, ChronoUnit.MINUTES))
            )
        ),
        FeedItem(
            id = "feed2",
            authorId = "contact2",
            authorName = "Jordan",
            contentType = ContentType.AGGREGATED_DATA,
            content = FeedContent.AggregatedData(
                title = "My mood this week",
                summary = "Interesting pattern - mood dips on Wednesdays but recovers by Friday",
                timeRange = "This week",
                dataPoints = 7
            ),
            timestamp = Instant.now().minus(5, ChronoUnit.HOURS),
            isRead = false,
            canReshare = false,
            privacyLevel = PrivacyLevel.CLOSE_FRIENDS,
            expiresAt = Instant.now().plus(1, ChronoUnit.WEEKS)
        ),
        FeedItem(
            id = "feed3",
            authorId = "contact3",
            authorName = "Sam",
            contentType = ContentType.JOURNAL_ENTRY,
            content = FeedContent.JournalEntry(
                title = "Morning reflections ☀️",
                excerpt = "Started the day with meditation and gratitude practice. Feeling centered and ready for whatever comes...",
                wordCount = 247,
                mood = "peaceful"
            ),
            timestamp = Instant.now().minus(1, ChronoUnit.DAYS),
            isRead = true,
            canReshare = true,
            privacyLevel = PrivacyLevel.FAMILY,
            expiresAt = Instant.now().plus(3, ChronoUnit.DAYS)
        ),
        FeedItem(
            id = "feed4",
            authorId = "contact4",
            authorName = "Alex",
            contentType = ContentType.DATA_INSIGHT,
            content = FeedContent.DataInsight(
                title = "Best sleep in weeks!",
                description = "Finally got a full night's rest",
                value = "8.5",
                unit = "hours",
                emoji = "😴",
                pluginId = "sleep"
            ),
            timestamp = Instant.now().minus(2, ChronoUnit.DAYS),
            isRead = true,
            canReshare = true,
            privacyLevel = PrivacyLevel.FRIENDS,
            expiresAt = Instant.now().plus(5, ChronoUnit.DAYS)
        ),
        FeedItem(
            id = "feed5",
            authorId = "contact1",
            authorName = "Cashka",
            contentType = ContentType.MILESTONE,
            content = FeedContent.MilestoneAchievement(
                title = "7-Day Streak!",
                description = "Completed water intake goal for 7 days straight",
                achievementType = "consistency",
                badgeEmoji = "🏆"
            ),
            timestamp = Instant.now().minus(3, ChronoUnit.DAYS),
            isRead = true,
            canReshare = true,
            privacyLevel = PrivacyLevel.FRIENDS,
            reactions = listOf(
                Reaction("contact3", "Sam", "🎉", Instant.now().minus(2, ChronoUnit.DAYS))
            )
        ),
        FeedItem(
            id = "feed6",
            authorId = "contact5",
            authorName = "Morgan",
            contentType = ContentType.DATA_INSIGHT,
            content = FeedContent.DataInsight(
                title = "Workout complete!",
                description = "Intense HIIT session this morning",
                value = "45",
                unit = "minutes",
                emoji = "💪",
                pluginId = "exercise"
            ),
            timestamp = Instant.now().minus(4, ChronoUnit.HOURS),
            isRead = false,
            canReshare = true,
            privacyLevel = PrivacyLevel.FRIENDS,
            expiresAt = Instant.now().plus(1, ChronoUnit.DAYS)
        )
    )
    
    private fun generateMockContactRequests() = listOf(
        ContactRequest(
            id = "req1",
            fromContactId = "new_contact_1",
            fromDisplayName = "Riley",
            message = "Hey! Found you through mutual friends. Would love to connect!",
            timestamp = Instant.now().minus(6, ChronoUnit.HOURS),
            status = RequestStatus.PENDING
        ),
        ContactRequest(
            id = "req2",
            fromContactId = "new_contact_2", 
            fromDisplayName = "Casey",
            message = null,
            timestamp = Instant.now().minus(1, ChronoUnit.DAYS),
            status = RequestStatus.PENDING
        )
    )
    
    private fun generateMockShareableContent() = listOf(
        ShareableDataPoint(
            id = "share1",
            pluginId = "water",
            pluginName = "Water Intake",
            title = "Today's hydration",
            preview = "2.1L water consumed",
            contentType = ContentType.DATA_INSIGHT,
            timestamp = Instant.now().minus(1, ChronoUnit.HOURS),
            privacyRisk = PrivacyRisk.LOW,
            dataSize = 1024L
        ),
        ShareableDataPoint(
            id = "share2",
            pluginId = "mood",
            pluginName = "Mood Tracker",
            title = "Current mood",
            preview = "Feeling great today! 😊",
            contentType = ContentType.DATA_INSIGHT,
            timestamp = Instant.now().minus(30, ChronoUnit.MINUTES),
            privacyRisk = PrivacyRisk.MEDIUM,
            dataSize = 512L
        ),
        ShareableDataPoint(
            id = "share3",
            pluginId = "exercise",
            pluginName = "Exercise Log",
            title = "Morning run",
            preview = "5.2km in 28 minutes",
            contentType = ContentType.DATA_INSIGHT,
            timestamp = Instant.now().minus(3, ChronoUnit.HOURS),
            privacyRisk = PrivacyRisk.LOW,
            dataSize = 2048L
        ),
        ShareableDataPoint(
            id = "share4",
            pluginId = "sleep",
            pluginName = "Sleep Tracker",
            title = "Last night's sleep",
            preview = "7.5 hours, quality: Good",
            contentType = ContentType.AGGREGATED_DATA,
            timestamp = Instant.now().minus(8, ChronoUnit.HOURS),
            privacyRisk = PrivacyRisk.MEDIUM,
            dataSize = 1536L
        )
    )
    
    private fun generateMockSharedContent() = listOf(
        FeedItem(
            id = "shared1",
            authorId = "me",
            authorName = "You",
            contentType = ContentType.DATA_INSIGHT,
            content = FeedContent.DataInsight(
                title = "Yesterday's steps",
                description = "Had a really active day!",
                value = "15,234",
                unit = "steps",
                emoji = "🚶‍♂️",
                pluginId = "steps"
            ),
            timestamp = Instant.now().minus(1, ChronoUnit.DAYS),
            isRead = true,
            canReshare = false,
            privacyLevel = PrivacyLevel.FRIENDS,
            expiresAt = Instant.now().plus(6, ChronoUnit.DAYS)
        )
    )
}
