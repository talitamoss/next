package com.domain.app.network.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * P2P Protocol definitions and message handling
 * 
 * File location: app/src/main/java/com/domain/app/network/protocol/P2PProtocol.kt
 */

/**
 * Base message structure for all P2P communications
 */
@Serializable
data class P2PMessage(
    val id: String = generateMessageId(),
    val type: MessageType,
    val payload: String,
    val timestamp: Long = System.currentTimeMillis(),
    val version: Int = PROTOCOL_VERSION
) {
    companion object {
        const val PROTOCOL_VERSION = 1
        
        fun generateMessageId(): String {
            return "msg_${System.currentTimeMillis()}_${(0..9999).random()}"
        }
    }
}

/**
 * Types of messages that can be sent
 */
@Serializable
enum class MessageType {
    // Basic connectivity
    PING,
    PONG,
    
    // Feed/content discovery
    FEED_REQUEST,
    FEED_RESPONSE,
    
    // Content transfer
    CONTENT_REQUEST,
    CONTENT_RESPONSE,
    
    // Data sharing
    DATA_SHARE,
    DATA_ACKNOWLEDGMENT,
    
    // Contact management
    PROFILE_UPDATE,
    STATUS_UPDATE,
    
    // Error handling
    ERROR
}

/**
 * Feed request - asking a peer for their available content
 */
@Serializable
data class FeedRequest(
    val requesterId: String,
    val since: Long? = null,  // Timestamp to get updates since
    val limit: Int = 50,      // Maximum items to return
    val contentTypes: List<String>? = null  // Filter by content type
)

/**
 * Feed response - list of available content
 */
@Serializable
data class FeedResponse(
    val items: List<FeedItem>,
    val hasMore: Boolean = false,
    val nextCursor: String? = null
)

/**
 * Individual feed item metadata
 */
@Serializable
data class FeedItem(
    val id: String,
    val contentId: String,
    val contentType: String,
    val title: String? = null,
    val preview: String? = null,
    val timestamp: Long,
    val size: Long? = null,
    val encrypted: Boolean = true
)

/**
 * Content metadata (not serializable, for internal use)
 */
data class ContentMetadata(
    val id: String,
    val contentId: String,
    val contentType: String,
    val timestamp: Long,
    val size: Long,
    val mimeType: String,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Request specific content
 */
@Serializable
data class ContentRequest(
    val contentId: String,
    val requestType: RequestType = RequestType.FULL
) {
    @Serializable
    enum class RequestType {
        METADATA_ONLY,  // Just the metadata
        PREVIEW,        // Limited preview
        FULL           // Complete content
    }
}

/**
 * Content response
 */
@Serializable
data class ContentResponse(
    val contentId: String,
    val requestType: ContentRequest.RequestType,
    val data: String? = null,  // Base64 encoded if binary
    val error: ErrorInfo? = null
)

/**
 * Share data with a peer
 */
@Serializable
data class DataShare(
    val dataType: String,
    val data: String,  // JSON encoded data
    val metadata: Map<String, String> = emptyMap(),
    val ephemeral: Boolean = false,  // Should peer delete after viewing?
    val expiresAt: Long? = null
)

/**
 * Error information
 */
@Serializable
data class ErrorInfo(
    val code: String,
    val message: String,
    val details: Map<String, String> = emptyMap()
)

/**
 * Protocol handler - manages encoding/decoding of messages
 */
@Singleton
class P2PProtocolHandler @Inject constructor() {
    
    private val json = Json { 
        ignoreUnknownKeys = true  // For backward compatibility
        prettyPrint = false       // Compact for network
        encodeDefaults = true
    }
    
    /**
     * Parse a raw message string
     */
    fun parseMessage(rawMessage: String): Result<P2PMessage> {
        return try {
            Result.success(json.decodeFromString(rawMessage))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Extract payload from a message
     */
    inline fun <reified T> extractPayload(message: P2PMessage): Result<T> {
        return try {
            Result.success(json.decodeFromString(message.payload))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Create a ping message to check if peer is alive
     */
    fun createPing(): String {
        val message = P2PMessage(
            type = MessageType.PING,
            payload = "{}"
        )
        return json.encodeToString(message)
    }
    
    /**
     * Create a pong response
     */
    fun createPong(pingId: String): String {
        val message = P2PMessage(
            type = MessageType.PONG,
            payload = json.encodeToString(mapOf("pingId" to pingId))
        )
        return json.encodeToString(message)
    }
    
    /**
     * Request feed from a peer
     */
    fun createFeedRequest(
        since: Instant? = null,
        limit: Int = 50,
        contentTypes: List<String>? = null
    ): String {
        val request = FeedRequest(
            requesterId = "self", // This should be passed in
            since = since?.toEpochMilli(),
            limit = limit,
            contentTypes = contentTypes
        )
        
        val message = P2PMessage(
            type = MessageType.FEED_REQUEST,
            payload = json.encodeToString(request)
        )
        
        return json.encodeToString(message)
    }
    
    /**
     * Create feed response
     */
    fun createFeedResponse(
        requestId: String,
        items: List<ContentMetadata>
    ): String {
        val feedItems = items.map { metadata ->
            FeedItem(
                id = UUID.randomUUID().toString(),
                contentId = metadata.contentId,
                contentType = metadata.contentType,
                timestamp = metadata.timestamp,
                size = metadata.size,
                encrypted = true
            )
        }
        
        val response = FeedResponse(
            items = feedItems,
            hasMore = false  // TODO: Implement pagination
        )
        
        val message = P2PMessage(
            id = requestId,
            type = MessageType.FEED_RESPONSE,
            payload = json.encodeToString(response)
        )
        
        return json.encodeToString(message)
    }
    
    /**
     * Request specific content
     */
    fun createContentRequest(
        contentId: String,
        requestType: ContentRequest.RequestType = ContentRequest.RequestType.FULL
    ): String {
        val request = ContentRequest(contentId, requestType)
        
        val message = P2PMessage(
            type = MessageType.CONTENT_REQUEST,
            payload = json.encodeToString(request)
        )
        
        return json.encodeToString(message)
    }
    
    /**
     * Create content response
     */
    fun createContentResponse(
        requestId: String,
        content: Any
    ): String {
        val response = when (content) {
            is ContentMetadata -> ContentResponse(
                contentId = content.contentId,
                requestType = ContentRequest.RequestType.METADATA_ONLY,
                data = json.encodeToString(content.metadata)
            )
            is ByteArray -> ContentResponse(
                contentId = requestId,
                requestType = ContentRequest.RequestType.FULL,
                data = content.encodeToBase64()
            )
            else -> ContentResponse(
                contentId = requestId,
                requestType = ContentRequest.RequestType.FULL,
                data = content.toString()
            )
        }
        
        val message = P2PMessage(
            id = requestId,
            type = MessageType.CONTENT_RESPONSE,
            payload = json.encodeToString(response)
        )
        
        return json.encodeToString(message)
    }
    
    /**
     * Share data with a peer
     */
    fun createDataShare(
        dataType: String,
        data: Any,
        ephemeral: Boolean = false,
        expiresIn: Long? = null
    ): String {
        val share = DataShare(
            dataType = dataType,
            data = json.encodeToString(data),
            ephemeral = ephemeral,
            expiresAt = expiresIn?.let { System.currentTimeMillis() + it }
        )
        
        val message = P2PMessage(
            type = MessageType.DATA_SHARE,
            payload = json.encodeToString(share)
        )
        
        return json.encodeToString(message)
    }
    
    /**
     * Create error message
     */
    fun createError(
        code: String,
        message: String,
        details: Map<String, String> = emptyMap()
    ): String {
        val error = ErrorInfo(code, message, details)
        
        val msg = P2PMessage(
            type = MessageType.ERROR,
            payload = json.encodeToString(error)
        )
        
        return json.encodeToString(msg)
    }
}

// Extension function to encode ByteArray to Base64
private fun ByteArray.encodeToBase64(): String {
    return java.util.Base64.getEncoder().encodeToString(this)
}
