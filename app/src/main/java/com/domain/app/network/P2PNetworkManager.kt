package com.domain.app.network

import com.domain.app.core.data.DataPoint
import com.domain.app.core.data.DataRepository
import com.domain.app.network.protocol.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Main P2P Network Manager - coordinates all P2P operations
 * 
 * File location: app/src/main/java/com/domain/app/network/P2PNetworkManager.kt
 */
@Singleton
class P2PNetworkManager @Inject constructor(
    private val p2pService: SimpleP2PService,
    private val protocol: P2PProtocolHandler,
    private val dataRepository: DataRepository,
    private val contentStore: ContentStore,
    private val coroutineScope: CoroutineScope
) {
    
    // Pending requests waiting for responses
    private val pendingRequests = ConcurrentHashMap<String, CompletableDeferred<P2PMessage>>()
    
    // Feed cache
    private val _feedItems = MutableStateFlow<List<FeedItem>>(emptyList())
    val feedItems: StateFlow<List<FeedItem>> = _feedItems.asStateFlow()
    
    // Network events
    private val _networkEvents = MutableSharedFlow<NetworkEvent>()
    val networkEvents: SharedFlow<NetworkEvent> = _networkEvents.asSharedFlow()
    
    init {
        // Set up message listener
        p2pService.addMessageListener { contactId, rawMessage ->
            coroutineScope.launch {
                handleIncomingMessage(contactId, rawMessage)
            }
        }
    }
    
    /**
     * Initialize the network manager
     */
    suspend fun initialize(nickname: String, password: String): Result<String> {
        return try {
            // Initialize P2P service
            p2pService.initialize()
            
            // Create or load account
            val linkResult = p2pService.createOrLoadAccount(nickname, password)
            
            linkResult.onSuccess { link ->
                _networkEvents.emit(NetworkEvent.Initialized(link))
            }
            
            linkResult
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Add a contact and establish connection
     */
    suspend fun addContact(contactLink: String, nickname: String? = null): Result<Contact> {
        return p2pService.addContact(contactLink).also { result ->
            result.onSuccess { contact ->
                // Store nickname locally if provided
                if (nickname != null) {
                    contentStore.setContactNickname(contact.id, nickname)
                }
                
                _networkEvents.emit(NetworkEvent.ContactAdded(contact))
                
                // Send initial ping to establish connection
                sendPing(contact.id)
            }
        }
    }
    
    /**
     * Get all contacts with their online status
     */
    suspend fun getContactsWithStatus(): List<ContactWithStatus> {
        val contacts = p2pService.getContacts().getOrElse { emptyList() }
        
        return contacts.map { contact ->
            ContactWithStatus(
                contact = contact,
                nickname = contentStore.getContactNickname(contact.id),
                isOnline = checkIfOnline(contact.id),
                lastSeen = contentStore.getLastSeen(contact.id)
            )
        }
    }
    
    /**
     * Refresh feed from all contacts
     */
    suspend fun refreshFeed() {
        _networkEvents.emit(NetworkEvent.FeedRefreshStarted)
        
        val contacts = p2pService.getContacts().getOrElse { emptyList() }
        val allItems = mutableListOf<FeedItem>()
        
        // Request feed from each contact
        coroutineScope {
            contacts.map { contact ->
                async {
                    requestFeedFromContact(contact.id)
                }
            }.awaitAll().forEach { items ->
                allItems.addAll(items)
            }
        }
        
        // Sort by timestamp and update state
        _feedItems.value = allItems.sortedByDescending { it.timestamp }
        _networkEvents.emit(NetworkEvent.FeedRefreshCompleted(allItems.size))
    }
    
    /**
     * Share data with specific contacts
     */
    suspend fun shareData(
        dataPoint: DataPoint,
        withContacts: List<String> = emptyList(),
        ephemeral: Boolean = false,
        expiresIn: Long? = null
    ): Result<Unit> {
        return try {
            // Store in content store first
            val contentId = contentStore.storeContent(dataPoint)
            
            // Create feed item for this content
            val feedItem = FeedItem(
                contentId = contentId,
                contentType = dataPoint.pluginId,
                title = dataPoint.metadata["title"] as? String,
                preview = createPreview(dataPoint),
                timestamp = dataPoint.timestamp.toEpochMilli(),
                encrypted = true
            )
            
            // Add to our available content
            contentStore.addAvailableContent(feedItem, withContacts)
            
            // Notify specific contacts if requested
            if (withContacts.isNotEmpty()) {
                val message = protocol.createDataShare(
                    dataType = dataPoint.pluginId,
                    data = dataPoint,
                    ephemeral = ephemeral,
                    expiresIn = expiresIn
                )
                
                withContacts.forEach { contactId ->
                    p2pService.sendMessage(contactId, message)
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Fetch specific content from a peer
     */
    suspend fun fetchContent(
        contentId: String,
        fromPeer: String,
        requestType: ContentRequest.RequestType = ContentRequest.RequestType.FULL
    ): Result<Any> {
        val request = protocol.createContentRequest(contentId, requestType)
        
        return sendRequestAndWaitForResponse(fromPeer, request) { response ->
            when (response.type) {
                MessageType.CONTENT_RESPONSE -> {
                    val contentResponse = protocol.extractPayload<ContentResponse>(response).getOrThrow()
                    if (contentResponse.error != null) {
                        Result.failure(Exception(contentResponse.error.message))
                    } else {
                        Result.success(contentResponse.data ?: "")
                    }
                }
                MessageType.ERROR -> {
                    val error = protocol.extractPayload<ErrorInfo>(response).getOrThrow()
                    Result.failure(Exception(error.message))
                }
                else -> Result.failure(Exception("Unexpected response type"))
            }
        }
    }
    
    /**
     * Handle incoming messages
     */
    private suspend fun handleIncomingMessage(contactId: String, rawMessage: String) {
        val parseResult = protocol.parseMessage(rawMessage)
        
        parseResult.fold(
            onSuccess = { message ->
                // Update last seen
                contentStore.updateLastSeen(contactId)
                
                // Check if this is a response to a pending request
                pendingRequests[message.id]?.complete(message)
                
                // Handle based on message type
                when (message.type) {
                    MessageType.PING -> handlePing(contactId, message)
                    MessageType.PONG -> handlePong(contactId, message)
                    MessageType.FEED_REQUEST -> handleFeedRequest(contactId, message)
                    MessageType.FEED_RESPONSE -> handleFeedResponse(contactId, message)
                    MessageType.CONTENT_REQUEST -> handleContentRequest(contactId, message)
                    MessageType.CONTENT_RESPONSE -> handleContentResponse(contactId, message)
                    MessageType.DATA_SHARE -> handleDataShare(contactId, message)
                    MessageType.ERROR -> handleError(contactId, message)
                    else -> {
                        // Unknown message type
                    }
                }
            },
            onFailure = { error ->
                _networkEvents.emit(NetworkEvent.Error(contactId, error))
            }
        )
    }
    
    /**
     * Handle ping request
     */
    private suspend fun handlePing(contactId: String, message: P2PMessage) {
        val pong = protocol.createPong(message.id)
        p2pService.sendMessage(contactId, pong)
    }
    
    /**
     * Handle pong response
     */
    private suspend fun handlePong(contactId: String, message: P2PMessage) {
        // Contact is online, update status
        contentStore.markContactOnline(contactId)
    }
    
    /**
     * Handle feed request from a peer
     */
    private suspend fun handleFeedRequest(contactId: String, message: P2PMessage) {
        val request = protocol.extractPayload<FeedRequest>(message).getOrThrow()
        
        // Get available content for this contact
        val availableItems = contentStore.getAvailableContentFor(contactId)
            .filter { item ->
                // Filter by timestamp if requested
                request.since?.let { since ->
                    item.timestamp > since
                } ?: true
            }
            .filter { item ->
                // Filter by content type if requested
                request.contentTypes?.contains(item.contentType) ?: true
            }
            .take(request.limit)
        
        // Send response
        val response = protocol.createFeedResponse(message.id, availableItems)
        p2pService.sendMessage(contactId, response)
    }
    
    /**
     * Handle feed response from a peer
     */
    private suspend fun handleFeedResponse(contactId: String, message: P2PMessage) {
        val response = protocol.extractPayload<FeedResponse>(message).getOrThrow()
        
        // Add items to feed cache
        response.items.forEach { item ->
            contentStore.addFeedItem(contactId, item)
        }
        
        _networkEvents.emit(NetworkEvent.FeedUpdated(contactId, response.items.size))
    }
    
    /**
     * Handle content request from a peer
     */
    private suspend fun handleContentRequest(contactId: String, message: P2PMessage) {
        val request = protocol.extractPayload<ContentRequest>(message).getOrThrow()
        
        // Check if content is available for this contact
        if (!contentStore.isContentAvailableFor(request.contentId, contactId)) {
            val error = protocol.createError(
                code = "CONTENT_NOT_AVAILABLE",
                message = "Content not available or access denied"
            )
            p2pService.sendMessage(contactId, error)
            return
        }
        
        // Get content based on request type
        val content = when (request.requestType) {
            ContentRequest.RequestType.METADATA_ONLY -> {
                contentStore.getContentMetadata(request.contentId)
            }
            ContentRequest.RequestType.PREVIEW -> {
                contentStore.getContentPreview(request.contentId)
            }
            ContentRequest.RequestType.FULL -> {
                contentStore.getFullContent(request.contentId)
            }
        }
        
        // Send response
        val response = ContentResponse(
            contentId = request.contentId,
            requestType = request.requestType,
            data = content
        )
        
        val responseMessage = P2PMessage(
            type = MessageType.CONTENT_RESPONSE,
            payload = protocol.json.encodeToString(response)
        )
        
        p2pService.sendMessage(contactId, protocol.json.encodeToString(responseMessage))
    }
    
    /**
     * Handle data share from a peer
     */
    private suspend fun handleDataShare(contactId: String, message: P2PMessage) {
        val share = protocol.extractPayload<DataShare>(message).getOrThrow()
        
        // Store received data
        contentStore.storeReceivedContent(contactId, share)
        
        // Send acknowledgment
        val ack = P2PMessage(
            type = MessageType.DATA_ACKNOWLEDGMENT,
            payload = """{"messageId":"${message.id}"}"""
        )
        p2pService.sendMessage(contactId, protocol.json.encodeToString(ack))
        
        _networkEvents.emit(NetworkEvent.DataReceived(contactId, share.dataType))
    }
    
    /**
     * Handle error message
     */
    private suspend fun handleError(contactId: String, message: P2PMessage) {
        val error = protocol.extractPayload<ErrorInfo>(message).getOrThrow()
        _networkEvents.emit(NetworkEvent.Error(contactId, Exception(error.message)))
    }
    
    /**
     * Send ping to check if contact is online
     */
    private suspend fun sendPing(contactId: String): Boolean {
        val ping = protocol.createPing()
        
        return try {
            withTimeout(5000) { // 5 second timeout
                sendRequestAndWaitForResponse(contactId, ping) { response ->
                    response.type == MessageType.PONG
                }
            }
            true
        } catch (e: TimeoutCancellationException) {
            false
        }
    }
    
    /**
     * Check if contact is online
     */
    private suspend fun checkIfOnline(contactId: String): Boolean {
        val lastSeen = contentStore.getLastSeen(contactId)
        
        return if (lastSeen != null && 
            Instant.now().minusSeconds(300).isBefore(lastSeen)) {
            // Seen in last 5 minutes, assume online
            true
        } else {
            // Try to ping
            sendPing(contactId)
        }
    }
    
    /**
     * Request feed from specific contact
     */
    private suspend fun requestFeedFromContact(contactId: String): List<FeedItem> {
        val request = protocol.createFeedRequest(
            since = contentStore.getLastFeedUpdate(contactId)
        )
        
        return try {
            sendRequestAndWaitForResponse(contactId, request) { response ->
                when (response.type) {
                    MessageType.FEED_RESPONSE -> {
                        val feedResponse = protocol.extractPayload<FeedResponse>(response).getOrThrow()
                        feedResponse.items
                    }
                    else -> emptyList()
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Send request and wait for response
     */
    private suspend fun <T> sendRequestAndWaitForResponse(
        contactId: String,
        request: String,
        timeout: Long = 30000,
        handler: (P2PMessage) -> T
    ): T {
        val messageId = P2PMessage.generateMessageId()
        val deferred = CompletableDeferred<P2PMessage>()
        
        // Store pending request
        pendingRequests[messageId] = deferred
        
        try {
            // Send request
            p2pService.sendMessage(contactId, request)
            
            // Wait for response
            val response = withTimeout(timeout) {
                deferred.await()
            }
            
            return handler(response)
        } finally {
            // Clean up
            pendingRequests.remove(messageId)
        }
    }
    
    /**
     * Create preview for data point
     */
    private fun createPreview(dataPoint: DataPoint): String {
        return when (dataPoint.pluginId) {
            "mood" -> "Mood update"
            "water" -> "Hydration tracked"
            "exercise" -> "Activity logged"
            else -> "New entry"
        }
    }
}

/**
 * Network events
 */
sealed class NetworkEvent {
    data class Initialized(val contactLink: String) : NetworkEvent()
    data class ContactAdded(val contact: Contact) : NetworkEvent()
    object FeedRefreshStarted : NetworkEvent()
    data class FeedRefreshCompleted(val itemCount: Int) : NetworkEvent()
    data class FeedUpdated(val contactId: String, val newItems: Int) : NetworkEvent()
    data class DataReceived(val fromContact: String, val dataType: String) : NetworkEvent()
    data class Error(val contactId: String?, val error: Throwable) : NetworkEvent()
}

/**
 * Contact with additional status information
 */
data class ContactWithStatus(
    val contact: Contact,
    val nickname: String?,
    val isOnline: Boolean,
    val lastSeen: Instant?
)
