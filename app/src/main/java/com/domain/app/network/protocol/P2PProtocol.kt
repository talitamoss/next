package com.domain.app.network.protocol

import com.domain.app.network.protocol.P2PProtocol.MessageType
import com.domain.app.network.protocol.P2PProtocol.json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of P2P protocol handler
 * 
 * File location: app/src/main/java/com/domain/app/network/protocol/P2PProtocolHandlerImpl.kt
 */
@Singleton
class P2PProtocolHandlerImpl @Inject constructor() : P2PProtocolHandler {
    
    override suspend fun handleMessage(message: P2PMessage): P2PMessage? {
        return when (message.type) {
            P2PProtocol.MessageType.PING -> handlePing(message)
            else -> {
                Timber.w("Unhandled message type: ${message.type}")
                null
            }
        }
    }
    
    override suspend fun onConnectionEstablished(peerId: String) {
        Timber.d("Connection established with peer: $peerId")
    }
    
    override suspend fun onConnectionLost(peerId: String) {
        Timber.d("Connection lost with peer: $peerId")
    }
    
    private fun handlePing(message: P2PMessage): P2PMessage {
        return P2PMessage(
            type = P2PProtocol.MessageType.PONG,
            senderId = "self", // Should be injected
            payload = ""
        )
    }
}

/**
 * Extension functions for P2P protocol
 */
object P2PProtocolExtensions {
    
    fun parseMessage(jsonString: String): P2PMessage? {
        return try {
            json.decodeFromString<P2PMessage>(jsonString)
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse P2P message")
            null
        }
    }
    
    fun createPing(senderId: String): P2PMessage {
        return P2PMessage(
            type = P2PProtocol.MessageType.PING,
            senderId = senderId,
            payload = ""
        )
    }
    
    fun createPong(senderId: String): P2PMessage {
        return P2PMessage(
            type = P2PProtocol.MessageType.PONG,
            senderId = senderId,
            payload = ""
        )
    }
    
    fun createError(senderId: String, error: String): P2PMessage {
        val errorInfo = ErrorInfo(
            code = "GENERIC_ERROR",
            message = error
        )
        
        return P2PMessage(
            type = P2PProtocol.MessageType.ERROR,
            senderId = senderId,
            payload = json.encodeToString(errorInfo)
        )
    }
    
    fun <T> extractPayload(message: P2PMessage, type: Class<T>): T? {
        return try {
            when (type) {
                FeedRequest::class.java -> json.decodeFromString<FeedRequest>(message.payload) as T
                FeedResponse::class.java -> json.decodeFromString<FeedResponse>(message.payload) as T
                ContentRequest::class.java -> json.decodeFromString<ContentRequest>(message.payload) as T
                ContentResponse::class.java -> json.decodeFromString<ContentResponse>(message.payload) as T
                DataShare::class.java -> json.decodeFromString<DataShare>(message.payload) as T
                ErrorInfo::class.java -> json.decodeFromString<ErrorInfo>(message.payload) as T
                else -> null
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to extract payload")
            null
        }
    }
    
    fun createFeedRequest(
        senderId: String,
        since: Long? = null,
        limit: Int = 50,
        contentTypes: List<String>? = null
    ): P2PMessage {
        val request = FeedRequest(
            requesterId = senderId,
            since = since,
            limit = limit,
            contentTypes = contentTypes
        )
        
        return P2PMessage(
            type = P2PProtocol.MessageType.FEED_REQUEST,
            senderId = senderId,
            payload = json.encodeToString(request)
        )
    }
    
    fun createFeedResponse(
        senderId: String,
        items: List<FeedItem>
    ): P2PMessage {
        val response = FeedResponse(
            items = items,
            hasMore = false
        )
        
        return P2PMessage(
            type = P2PProtocol.MessageType.FEED_RESPONSE,
            senderId = senderId,
            payload = json.encodeToString(response)
        )
    }
    
    fun createContentResponse(
        senderId: String,
        contentId: String,
        requestType: ContentRequest.RequestType,
        data: String? = null,
        error: ErrorInfo? = null
    ): P2PMessage {
        val response = ContentResponse(
            contentId = contentId,
            requestType = requestType,
            data = data,
            error = error
        )
        
        return P2PMessage(
            type = P2PProtocol.MessageType.CONTENT_RESPONSE,
            senderId = senderId,
            payload = json.encodeToString(response)
        )
    }
}

/**
 * Error information structure
 */
@kotlinx.serialization.Serializable
data class ErrorInfo(
    val code: String,
    val message: String,
    val details: Map<String, String> = emptyMap()
)
