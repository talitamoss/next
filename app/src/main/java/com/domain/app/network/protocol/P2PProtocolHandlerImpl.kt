package com.domain.app.network.protocol

import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of P2P protocol message handling
 * Manages encoding/decoding of all P2P communication messages
 * 
 * File location: app/src/main/java/com/domain/app/network/protocol/P2PProtocolHandlerImpl.kt
 */
@Singleton
class P2PProtocolHandlerImpl @Inject constructor() : P2PProtocolHandler {
    
    private val json = Json { 
        ignoreUnknownKeys = true  // For backward compatibility
        prettyPrint = false       // Compact for network transmission
        encodeDefaults = true
    }
    
    /**
     * Parse a raw message string into a P2P message
     */
    override fun parseMessage(rawMessage: String): Result<P2PMessage> {
        return try {
            Result.success(json.decodeFromString(rawMessage))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Extract typed payload from a message using class type
     */
    override fun <T> extractPayload(message: P2PMessage, type: Class<T>): Result<T> {
        return try {
            val decoded = when (type) {
                FeedRequest::class.java -> json.decodeFromString<FeedRequest>(message.payload)
                FeedResponse::class.java -> json.decodeFromString<FeedResponse>(message.payload)
                ContentRequest::class.java -> json.decodeFromString<ContentRequest>(message.payload)
                ContentResponse::class.java -> json.decodeFromString<ContentResponse>(message.payload)
                DataShare::class.java -> json.decodeFromString<DataShare>(message.payload)
                ErrorInfo::class.java -> json.decodeFromString<ErrorInfo>(message.payload)
                Map::class.java -> json.decodeFromString<Map<String, String>>(message.payload)
                else -> throw IllegalArgumentException("Unsupported payload type: $type")
            }
            @Suppress("UNCHECKED_CAST")
            Result.success(decoded as T)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Create a ping message for connectivity checks
     */
    override fun createPing(): String {
        val message = P2PMessage(
            type = MessageType.PING,
            payload = "{}"
        )
        return json.encodeToString(message)
    }
    
    /**
     * Create a pong response to a ping
     */
    override fun createPong(pingId: String): String {
        val message = P2PMessage(
            type = MessageType.PONG,
            payload = json.encodeToString(mapOf("pingId" to pingId))
        )
        return json.encodeToString(message)
    }
    
    /**
     * Create a feed request message
     */
    override fun createFeedRequest(
        requesterId: String,
        since: Long?,
        limit: Int,
        contentTypes: List<String>?
    ): String {
        val request = FeedRequest(
            requesterId = requesterId,
            since = since,
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
     * Create a feed response message
     */
    override fun createFeedResponse(
        requestId: String,
        items: List<ContentMetadata>
    ): String {
        val feedItems = items.map { metadata ->
            FeedItem(
                id = metadata.id,
                contentId = metadata.contentId,
                contentType = metadata.contentType,
                title = metadata.metadata["title"],
                preview = metadata.metadata["preview"],
                timestamp = metadata.timestamp,
                size = metadata.size
            )
        }
        
        val response = FeedResponse(
            items = feedItems,
            hasMore = false
        )
        
        val message = P2PMessage(
            id = requestId,
            type = MessageType.FEED_RESPONSE,
            payload = json.encodeToString(response)
        )
        
        return json.encodeToString(message)
    }
    
    /**
     * Create a content request message
     */
    override fun createContentRequest(
        contentId: String,
        requestType: ContentRequest.RequestType
    ): String {
        val request = ContentRequest(
            contentId = contentId,
            requestType = requestType
        )
        
        val message = P2PMessage(
            type = MessageType.CONTENT_REQUEST,
            payload = json.encodeToString(request)
        )
        
        return json.encodeToString(message)
    }
    
    /**
     * Create a content response message
     */
    override fun createContentResponse(
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
     * Create a data share message
     */
    override fun createDataShare(
        dataType: String,
        data: Any,
        ephemeral: Boolean,
        expiresIn: Long?
    ): String {
        val share = DataShare(
            dataType = dataType,
            data = when (data) {
                is String -> data
                else -> json.encodeToString(data)
            },
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
     * Create an error message
     */
    override fun createError(
        code: String,
        message: String,
        details: Map<String, String>
    ): String {
        val error = ErrorInfo(code, message, details)
        
        val msg = P2PMessage(
            type = MessageType.ERROR,
            payload = json.encodeToString(error)
        )
        
        return json.encodeToString(msg)
    }
}

/**
 * Extension function to encode ByteArray to Base64
 */
private fun ByteArray.encodeToBase64(): String {
    return java.util.Base64.getEncoder().encodeToString(this)
}
