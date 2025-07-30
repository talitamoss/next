package com.domain.app.network

import com.domain.app.core.data.DataPoint
import com.domain.app.network.protocol.DataShare
import com.domain.app.network.protocol.FeedItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local storage for P2P content and metadata
 * 
 * File location: app/src/main/java/com/domain/app/network/ContentStore.kt
 */
@Singleton
class ContentStore @Inject constructor() {
    
    // Contact nicknames (local only)
    private val contactNicknames = ConcurrentHashMap<String, String>()
    
    // Contact last seen timestamps
    private val contactLastSeen = ConcurrentHashMap<String, Instant>()
    
    // Available content for sharing
    private val availableContent = ConcurrentHashMap<String, AvailableContent>()
    
    // Feed items from contacts
    private val feedCache = ConcurrentHashMap<String, MutableList<FeedItem>>()
    
    // Received content from peers
    private val receivedContent = ConcurrentHashMap<String, ReceivedContent>()
    
    // Content access permissions
    private val contentPermissions = ConcurrentHashMap<String, Set<String>>()
    
    // Last feed update times
    private val lastFeedUpdate = ConcurrentHashMap<String, Instant>()
    
    private val json = Json { 
        prettyPrint = false
        ignoreUnknownKeys = true 
    }
    
    /**
     * Store content and return its ID
     */
    fun storeContent(dataPoint: DataPoint): String {
        val contentId = generateContentId()
        val content = AvailableContent(
            id = contentId,
            dataPoint = dataPoint,
            createdAt = Instant.now()
        )
        
        availableContent[contentId] = content
        return contentId
    }
    
    /**
     * Add content to available list with optional access restrictions
     */
    fun addAvailableContent(
        feedItem: FeedItem,
        allowedContacts: List<String> = emptyList()
    ) {
        if (allowedContacts.isNotEmpty()) {
            contentPermissions[feedItem.contentId] = allowedContacts.toSet()
        }
        // If no specific contacts listed, it's available to all
    }
    
    /**
     * Get available content for a specific contact
     */
    fun getAvailableContentFor(contactId: String): List<FeedItem> {
        return availableContent.values
            .filter { content ->
                // Check if this contact has permission
                val permissions = contentPermissions[content.id]
                permissions == null || permissions.contains(contactId)
            }
            .map { content ->
                FeedItem(
                    contentId = content.id,
                    contentType = content.dataPoint.pluginId,
                    title = content.dataPoint.metadata["title"] as? String,
                    preview = createPreview(content.dataPoint),
                    timestamp = content.createdAt.toEpochMilli(),
                    encrypted = true
                )
            }
            .sortedByDescending { it.timestamp }
    }
    
    /**
     * Check if content is available for a contact
     */
    fun isContentAvailableFor(contentId: String, contactId: String): Boolean {
        val permissions = contentPermissions[contentId]
        return permissions == null || permissions.contains(contactId)
    }
    
    /**
     * Get content metadata only
     */
    fun getContentMetadata(contentId: String): String? {
        val content = availableContent[contentId] ?: return null
        
        val metadata = mapOf(
            "id" to contentId,
            "type" to content.dataPoint.pluginId,
            "timestamp" to content.createdAt.toEpochMilli(),
            "hasData" to true
        )
        
        return json.encodeToString(metadata)
    }
    
    /**
     * Get content preview
     */
    fun getContentPreview(contentId: String): String? {
        val content = availableContent[contentId] ?: return null
        
        val preview = mapOf(
            "id" to contentId,
            "type" to content.dataPoint.pluginId,
            "preview" to createPreview(content.dataPoint),
            "timestamp" to content.createdAt.toEpochMilli()
        )
        
        return json.encodeToString(preview)
    }
    
    /**
     * Get full content
     */
    fun getFullContent(contentId: String): String? {
        val content = availableContent[contentId] ?: return null
        return json.encodeToString(content.dataPoint)
    }
    
    /**
     * Store received content from a peer
     */
    fun storeReceivedContent(fromContact: String, dataShare: DataShare) {
        val contentId = generateContentId()
        val received = ReceivedContent(
            id = contentId,
            fromContact = fromContact,
            dataShare = dataShare,
            receivedAt = Instant.now()
        )
        
        receivedContent[contentId] = received
        
        // Create feed item for display
        val feedItem = FeedItem(
            contentId = contentId,
            contentType = dataShare.dataType,
            title = dataShare.metadata["title"],
            preview = dataShare.metadata["preview"] ?: "Shared ${dataShare.dataType}",
            timestamp = Instant.now().toEpochMilli(),
            encrypted = true
        )
        
        addFeedItem(fromContact, feedItem)
    }
    
    /**
     * Add feed item from a contact
     */
    fun addFeedItem(contactId: String, item: FeedItem) {
        feedCache.computeIfAbsent(contactId) { mutableListOf() }.add(item)
    }
    
    /**
     * Get all feed items
     */
    fun getAllFeedItems(): List<Pair<String, FeedItem>> {
        return feedCache.flatMap { (contactId, items) ->
            items.map { item -> contactId to item }
        }.sortedByDescending { it.second.timestamp }
    }
    
    /**
     * Set contact nickname
     */
    fun setContactNickname(contactId: String, nickname: String) {
        contactNicknames[contactId] = nickname
    }
    
    /**
     * Get contact nickname
     */
    fun getContactNickname(contactId: String): String? {
        return contactNicknames[contactId]
    }
    
    /**
     * Update last seen time for contact
     */
    fun updateLastSeen(contactId: String) {
        contactLastSeen[contactId] = Instant.now()
    }
    
    /**
     * Mark contact as online
     */
    fun markContactOnline(contactId: String) {
        updateLastSeen(contactId)
    }
    
    /**
     * Get last seen time
     */
    fun getLastSeen(contactId: String): Instant? {
        return contactLastSeen[contactId]
    }
    
    /**
     * Get last feed update time
     */
    fun getLastFeedUpdate(contactId: String): Instant? {
        return lastFeedUpdate[contactId]
    }
    
    /**
     * Update last feed update time
     */
    fun updateLastFeedUpdate(contactId: String) {
        lastFeedUpdate[contactId] = Instant.now()
    }
    
    /**
     * Clear old content based on age
     */
    fun cleanupOldContent(olderThan: Instant) {
        // Remove old available content
        availableContent.entries.removeIf { (_, content) ->
            content.createdAt.isBefore(olderThan)
        }
        
        // Remove old received content
        receivedContent.entries.removeIf { (_, content) ->
            content.receivedAt.isBefore(olderThan)
        }
        
        // Clean up feed cache
        feedCache.forEach { (_, items) ->
            items.removeIf { item ->
                Instant.ofEpochMilli(item.timestamp).isBefore(olderThan)
            }
        }
    }
    
    /**
     * Generate unique content ID
     */
    private fun generateContentId(): String {
        return "content_${System.currentTimeMillis()}_${(0..99999).random()}"
    }
    
    /**
     * Create preview text for data point
     */
    private fun createPreview(dataPoint: DataPoint): String {
        return when (dataPoint.pluginId) {
            "mood" -> {
                val mood = dataPoint.value["mood"] as? String
                if (mood != null) "Feeling $mood" else "Mood update"
            }
            "water" -> {
                val amount = dataPoint.value["amount"] as? Number
                if (amount != null) "Drank ${amount}ml" else "Hydration tracked"
            }
            "exercise" -> {
                val type = dataPoint.value["type"] as? String
                if (type != null) "$type completed" else "Activity logged"
            }
            "note" -> {
                val text = dataPoint.value["text"] as? String
                text?.take(50)?.plus("...") ?: "Note added"
            }
            else -> "New ${dataPoint.pluginId} entry"
        }
    }
}

/**
 * Available content wrapper
 */
private data class AvailableContent(
    val id: String,
    val dataPoint: DataPoint,
    val createdAt: Instant
)

/**
 * Received content wrapper
 */
private data class ReceivedContent(
    val id: String,
    val fromContact: String,
    val dataShare: DataShare,
    val receivedAt: Instant
)
