package com.domain.app.social.contracts

import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Core contracts for social functionality
 * These interfaces define the boundary between UI and infrastructure layers
 */

// ===== DATA MODELS =====

data class SocialContact(
    val id: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val isOnline: Boolean = false,
    val lastSeen: Instant? = null,
    val trustLevel: TrustLevel = TrustLevel.FRIEND,
    val connectionQuality: ConnectionQuality = ConnectionQuality.GOOD
)

data class FeedItem(
    val id: String,
    val authorId: String,
    val authorName: String,
    val contentType: ContentType,
    val content: FeedContent,
    val timestamp: Instant,
    val isRead: Boolean = false,
    val canReshare: Boolean = false,
    val expiresAt: Instant? = null,
    val privacyLevel: PrivacyLevel = PrivacyLevel.FRIENDS,
    val reactions: List<Reaction> = emptyList()
)

sealed class FeedContent {
    data class DataInsight(
        val title: String,
        val description: String,
        val value: String,
        val unit: String? = null,
        val emoji: String? = null,
        val pluginId: String
    ) : FeedContent()
    
    data class JournalEntry(
        val title: String,
        val excerpt: String,
        val wordCount: Int,
        val mood: String? = null
    ) : FeedContent()
    
    data class MediaContent(
        val mediaUrl: String,
        val caption: String,
        val mediaType: MediaType,
        val thumbnailUrl: String? = null
    ) : FeedContent()
    
    data class AggregatedData(
        val title: String,
        val summary: String,
        val chartData: String? = null, // JSON representation
        val timeRange: String,
        val dataPoints: Int
    ) : FeedContent()
    
    data class MilestoneAchievement(
        val title: String,
        val description: String,
        val achievementType: String,
        val badgeEmoji: String
    ) : FeedContent()
}

data class Reaction(
    val userId: String,
    val userName: String,
    val emoji: String,
    val timestamp: Instant
)

enum class ContentType {
    DATA_INSIGHT, JOURNAL_ENTRY, MEDIA, AGGREGATED_DATA, MILESTONE, CUSTOM
}

enum class MediaType {
    IMAGE, VIDEO, AUDIO
}

enum class TrustLevel {
    BLOCKED, STRANGER, ACQUAINTANCE, FRIEND, CLOSE_FRIEND, FAMILY
}

enum class PrivacyLevel {
    PUBLIC, FRIENDS, CLOSE_FRIENDS, FAMILY, CUSTOM
}

enum class ConnectionQuality {
    EXCELLENT, GOOD, POOR, OFFLINE
}

data class SharingPermissions(
    val allowedContacts: List<String> = emptyList(),
    val allowedGroups: List<String> = emptyList(),
    val privacyLevel: PrivacyLevel = PrivacyLevel.FRIENDS,
    val expiresAt: Instant? = null,
    val allowResharing: Boolean = false,
    val requiresApproval: Boolean = false,
    val geoRestrictions: List<String> = emptyList()
)

data class ShareableDataPoint(
    val id: String,
    val pluginId: String,
    val pluginName: String,
    val title: String,
    val preview: String,
    val contentType: ContentType,
    val timestamp: Instant,
    val privacyRisk: PrivacyRisk = PrivacyRisk.LOW,
    val dataSize: Long = 0L,
    val canExpire: Boolean = true
)

enum class PrivacyRisk {
    LOW, MEDIUM, HIGH, CRITICAL
}

data class SocialStats(
    val totalFriends: Int,
    val onlineFriends: Int,
    val unreadItems: Int,
    val itemsSharedToday: Int,
    val itemsReceivedToday: Int
)

data class ContactRequest(
    val id: String,
    val fromContactId: String,
    val fromDisplayName: String,
    val message: String? = null,
    val timestamp: Instant,
    val status: RequestStatus = RequestStatus.PENDING
)

enum class RequestStatus {
    PENDING, ACCEPTED, DECLINED, EXPIRED
}

// ===== REPOSITORY INTERFACES =====

interface SocialRepository {
    // Contacts Management
    fun getContacts(): Flow<List<SocialContact>>
    suspend fun addContact(contactCode: String): Result<SocialContact>
    suspend fun removeContact(contactId: String): Result<Unit>
    suspend fun blockContact(contactId: String): Result<Unit>
    suspend fun updateContactTrustLevel(contactId: String, trustLevel: TrustLevel): Result<Unit>
    fun getContactRequests(): Flow<List<ContactRequest>>
    suspend fun acceptContactRequest(requestId: String): Result<Unit>
    suspend fun declineContactRequest(requestId: String): Result<Unit>
    
    // Feed Management
    fun getFeed(): Flow<List<FeedItem>>
    suspend fun refreshFeed(): Result<Unit>
    suspend fun markAsRead(itemId: String)
    suspend fun markAllAsRead(): Result<Unit>
    fun getUnreadCount(): Flow<Int>
    
    // Content Sharing
    suspend fun shareContent(dataPointId: String, permissions: SharingPermissions): Result<Unit>
    fun getShareableContent(): Flow<List<ShareableDataPoint>>
    suspend fun deleteSharedContent(shareId: String): Result<Unit>
    fun getSharedContent(): Flow<List<FeedItem>> // Content I've shared
    
    // Reactions & Interactions
    suspend fun addReaction(itemId: String, emoji: String): Result<Unit>
    suspend fun removeReaction(itemId: String): Result<Unit>
    suspend fun reshareContent(itemId: String, permissions: SharingPermissions): Result<Unit>
    
    // Statistics & Analytics
    fun getSocialStats(): Flow<SocialStats>
    suspend fun getContactActivity(contactId: String): Result<List<FeedItem>>
    
    // Privacy & Security
    suspend fun updatePrivacySettings(settings: PrivacySettings): Result<Unit>
    fun getPrivacySettings(): Flow<PrivacySettings>
    suspend fun reportContent(itemId: String, reason: String): Result<Unit>
}

data class PrivacySettings(
    val defaultPrivacyLevel: PrivacyLevel = PrivacyLevel.FRIENDS,
    val allowContactRequests: Boolean = true,
    val requireApprovalForSharing: Boolean = false,
    val defaultExpirationHours: Int? = 24,
    val allowResharing: Boolean = true,
    val showOnlineStatus: Boolean = true,
    val allowLocationSharing: Boolean = false
)
