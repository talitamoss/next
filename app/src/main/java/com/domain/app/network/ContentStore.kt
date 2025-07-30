package com.domain.app.network

import com.domain.app.network.protocol.ContentMetadata
import com.domain.app.network.protocol.FeedItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Content store for managing shared data in the P2P network
 * Handles content availability, caching, and access control
 * 
 * File location: app/src/main/java/com/domain/app/network/ContentStore.kt
 */
@Singleton
class ContentStore @Inject constructor() {
    
    // Content metadata storage
    private val contentMetadata = ConcurrentHashMap<String, ContentMetadata>()
    
    // Content data storage (in-memory cache)
    private val contentData = ConcurrentHashMap<String, ByteArray>()
    
    // Feed items by peer
    private val peerFeeds = ConcurrentHashMap<String, MutableList<FeedItem>>()
    
    // Access control - which peers can access which content
    private val contentAccess = ConcurrentHashMap<String, MutableSet<String>>()
    
    // Observable state of available content
    private val _availableContent = MutableStateFlow<List<ContentMetadata>>(emptyList())
    val availableContent: StateFlow<List<ContentMetadata>> = _availableContent.asStateFlow()
    
    /**
     * Add content to the store
     */
    fun addContent(metadata: ContentMetadata, data: ByteArray? = null) {
        contentMetadata[metadata.id] = metadata
        data?.let { contentData[metadata.id] = it }
        
        // By default, make content available to all peers
        contentAccess.getOrPut(metadata.id) { mutableSetOf() }.add("*")
        
        updateAvailableContent()
    }
    
    /**
     * Get content metadata
     */
    fun getContentMetadata(contentId: String): ContentMetadata? {
        return contentMetadata[contentId]
    }
    
    /**
     * Get content preview (first 256 bytes or less)
     */
    fun getContentPreview(contentId: String): ByteArray? {
        return contentData[contentId]?.let { data ->
            if (data.size <= 256) data else data.copyOf(256)
        }
    }
    
    /**
     * Get full content data
     */
    fun getFullContent(contentId: String): ByteArray? {
        return contentData[contentId]
    }
    
    /**
     * Check if content is available for a specific peer
     */
    fun isContentAvailableFor(contentId: String, peerId: String): Boolean {
        val accessSet = contentAccess[contentId] ?: return false
        return accessSet.contains("*") || accessSet.contains(peerId)
    }
    
    /**
     * Get all content available for a peer
     */
    fun getAvailableContent(peerId: String): List<ContentMetadata> {
        return contentMetadata.values.filter { metadata ->
            isContentAvailableFor(metadata.id, peerId)
        }
    }
    
    /**
     * Add a feed item from a peer
     */
    fun addFeedItem(peerId: String, item: FeedItem) {
        peerFeeds.getOrPut(peerId) { mutableListOf() }.add(item)
    }
    
    /**
     * Get feed items from a specific peer
     */
    fun getFeedItems(peerId: String): List<FeedItem> {
        return peerFeeds[peerId]?.toList() ?: emptyList()
    }
    
    /**
     * Get all feed items from all peers
     */
    fun getAllFeedItems(): List<FeedItem> {
        return peerFeeds.values.flatten()
    }
    
    /**
     * Clear feed items older than specified timestamp
     */
    fun clearOldFeedItems(olderThan: Long) {
        peerFeeds.forEach { (_, items) ->
            items.removeAll { it.timestamp < olderThan }
        }
    }
    
    /**
     * Set access control for content
     */
    fun setContentAccess(contentId: String, allowedPeers: Set<String>) {
        contentAccess[contentId] = allowedPeers.toMutableSet()
    }
    
    /**
     * Grant access to content for a specific peer
     */
    fun grantAccess(contentId: String, peerId: String) {
        contentAccess.getOrPut(contentId) { mutableSetOf() }.add(peerId)
    }
    
    /**
     * Revoke access to content for a specific peer
     */
    fun revokeAccess(contentId: String, peerId: String) {
        contentAccess[contentId]?.remove(peerId)
    }
    
    /**
     * Remove content from the store
     */
    fun removeContent(contentId: String) {
        contentMetadata.remove(contentId)
        contentData.remove(contentId)
        contentAccess.remove(contentId)
        updateAvailableContent()
    }
    
    /**
     * Clear all content from a specific peer
     */
    fun clearPeerContent(peerId: String) {
        peerFeeds.remove(peerId)
    }
    
    /**
     * Get storage statistics
     */
    fun getStorageStats(): StorageStats {
        val totalSize = contentData.values.sumOf { it.size.toLong() }
        val contentCount = contentMetadata.size
        val peerCount = peerFeeds.size
        val feedItemCount = peerFeeds.values.sumOf { it.size }
        
        return StorageStats(
            totalSizeBytes = totalSize,
            contentCount = contentCount,
            peerCount = peerCount,
            feedItemCount = feedItemCount
        )
    }
    
    /**
     * Update the observable state of available content
     */
    private fun updateAvailableContent() {
        _availableContent.value = contentMetadata.values.toList()
    }
}

/**
 * Storage statistics
 */
data class StorageStats(
    val totalSizeBytes: Long,
    val contentCount: Int,
    val peerCount: Int,
    val feedItemCount: Int
)
