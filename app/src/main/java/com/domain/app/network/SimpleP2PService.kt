package com.domain.app.network

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.briarproject.bramble.api.contact.ContactId
import org.briarproject.briar.api.BriarApplication
import org.briarproject.briar.api.BriarApplicationImpl
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Core P2P networking service using Briar
 * 
 * File location: app/src/main/java/com/domain/app/network/SimpleP2PService.kt
 */
@Singleton
class SimpleP2PService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var briarApp: BriarApplication? = null
    private val _connectionState = MutableStateFlow(ConnectionState.NOT_STARTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private val messageListeners = mutableListOf<(String, String) -> Unit>()
    
    /**
     * Initialize Briar P2P service
     * Call this from Application.onCreate() or MainActivity
     */
    suspend fun initialize() = withContext(Dispatchers.IO) {
        try {
            _connectionState.value = ConnectionState.STARTING
            
            // Create Briar data directory
            val appDir = File(context.applicationContext.filesDir, "briar")
            if (!appDir.exists()) {
                appDir.mkdirs()
            }
            
            // Initialize Briar application
            briarApp = BriarApplicationImpl(context, appDir)
            
            // Start Briar and wait for it to be ready
            briarApp?.startAndWait()
            
            // Set up message listeners
            setupMessageListeners()
            
            _connectionState.value = ConnectionState.READY
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.ERROR
            throw P2PException("Failed to initialize P2P service", e)
        }
    }
    
    /**
     * Create or load existing account
     * @param nickname Display name for this device/user
     * @param password Password for local encryption
     * @return Contact link that others can use to add you
     */
    suspend fun createOrLoadAccount(
        nickname: String, 
        password: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val accountManager = briarApp?.accountManager 
                ?: return@withContext Result.failure(NotInitializedException())
            
            // Check if account already exists
            if (!accountManager.accountExists()) {
                // Create new account
                accountManager.createAccount(nickname, password)
            } else {
                // Sign in to existing account
                accountManager.signIn(password)
            }
            
            // Wait for account to be ready
            accountManager.waitForStartup()
            
            // Get contact link for sharing
            val contactManager = briarApp?.contactManager
                ?: return@withContext Result.failure(NotInitializedException())
            
            val link = contactManager.getHandshakeLink()
            Result.success(link)
        } catch (e: Exception) {
            Result.failure(P2PException("Failed to create/load account", e))
        }
    }
    
    /**
     * Add a contact using their contact link
     * @param contactLink The link shared by the other person
     * @return The newly added contact
     */
    suspend fun addContact(contactLink: String): Result<Contact> = withContext(Dispatchers.IO) {
        try {
            val contactManager = briarApp?.contactManager
                ?: return@withContext Result.failure(NotInitializedException())
            
            // Parse and validate the contact link
            val pendingContact = contactManager.addPendingContact(contactLink)
            
            // Create our contact model
            val contact = Contact(
                id = pendingContact.id.toString(),
                alias = pendingContact.alias,
                status = ContactStatus.PENDING,
                addedAt = System.currentTimeMillis()
            )
            
            Result.success(contact)
        } catch (e: Exception) {
            Result.failure(P2PException("Failed to add contact", e))
        }
    }
    
    /**
     * Get all contacts
     */
    suspend fun getContacts(): Result<List<Contact>> = withContext(Dispatchers.IO) {
        try {
            val contactManager = briarApp?.contactManager
                ?: return@withContext Result.failure(NotInitializedException())
            
            val contacts = contactManager.getContacts().map { briarContact ->
                Contact(
                    id = briarContact.id.toString(),
                    alias = briarContact.author.name,
                    status = if (briarContact.isConnected) {
                        ContactStatus.CONNECTED
                    } else {
                        ContactStatus.DISCONNECTED
                    },
                    addedAt = briarContact.timeAdded
                )
            }
            
            Result.success(contacts)
        } catch (e: Exception) {
            Result.failure(P2PException("Failed to get contacts", e))
        }
    }
    
    /**
     * Send a message to a contact
     * @param contactId The contact's ID
     * @param message The message content (will be converted to bytes)
     */
    suspend fun sendMessage(
        contactId: String, 
        message: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val messagingManager = briarApp?.messagingManager
                ?: return@withContext Result.failure(NotInitializedException())
            
            val contact = ContactId(contactId)
            val messageBytes = message.toByteArray(Charsets.UTF_8)
            
            messagingManager.sendPrivateMessage(contact, messageBytes)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(P2PException("Failed to send message", e))
        }
    }
    
    /**
     * Register a listener for incoming messages
     * @param listener Callback that receives (contactId, message) pairs
     */
    fun addMessageListener(listener: (String, String) -> Unit) {
        messageListeners.add(listener)
    }
    
    /**
     * Remove a message listener
     */
    fun removeMessageListener(listener: (String, String) -> Unit) {
        messageListeners.remove(listener)
    }
    
    /**
     * Set up Briar message listeners
     */
    private fun setupMessageListeners() {
        briarApp?.messagingManager?.addPrivateMessageListener { message ->
            val contactId = message.contactId.toString()
            val content = String(message.body, Charsets.UTF_8)
            
            // Notify all registered listeners
            messageListeners.forEach { listener ->
                listener(contactId, content)
            }
        }
    }
    
    /**
     * Clean up resources
     */
    suspend fun shutdown() = withContext(Dispatchers.IO) {
        try {
            briarApp?.stop()
            _connectionState.value = ConnectionState.NOT_STARTED
        } catch (e: Exception) {
            // Log but don't throw - we're shutting down anyway
        }
    }
    
    /**
     * Get current connection state
     */
    fun isReady(): Boolean = connectionState.value == ConnectionState.READY
}

/**
 * Connection states for the P2P service
 */
enum class ConnectionState {
    NOT_STARTED,
    STARTING,
    READY,
    ERROR
}

/**
 * Contact model
 */
data class Contact(
    val id: String,
    val alias: String,
    val status: ContactStatus,
    val addedAt: Long
)

/**
 * Contact connection status
 */
enum class ContactStatus {
    PENDING,
    CONNECTED,
    DISCONNECTED,
    BLOCKED
}

/**
 * Custom exceptions
 */
class P2PException(message: String, cause: Throwable? = null) : Exception(message, cause)
class NotInitializedException : Exception("P2P service not initialized")
