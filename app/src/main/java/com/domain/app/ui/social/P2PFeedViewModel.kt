package com.domain.app.ui.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domain.app.network.p2p.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for pull-based P2P feed
 * 
 * File location: app/src/main/java/com/domain/app/ui/social/P2PFeedViewModel.kt
 */
@HiltViewModel
class P2PFeedViewModel @Inject constructor(
    private val p2pMessaging: SimpleP2PMessaging
) : ViewModel() {
    
    // UI State
    private val _uiState = MutableStateFlow(P2PFeedUiState())
    val uiState: StateFlow<P2PFeedUiState> = _uiState.asStateFlow()
    
    // Feed items (combined local + pulled from peers)
    val feedItems: StateFlow<List<FeedItem>> = p2pMessaging.getAggregatedFeed()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // Connected peers
    val connectedPeers = p2pMessaging.connectedPeers
    
    init {
        // Initialize P2P
        p2pMessaging.initialize()
        
        // Auto-refresh feed periodically (every 30 seconds)
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(30_000)
                pullFeed()
            }
        }
    }
    
    /**
     * Post a new message (make available for peers to pull)
     */
    fun postMessage(content: String) {
        if (content.isBlank()) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isPosting = true) }
            
            try {
                p2pMessaging.postMessage(content)
                _uiState.update { 
                    it.copy(
                        isPosting = false,
                        messageInput = "" // Clear input
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isPosting = false,
                        error = "Failed to post message"
                    )
                }
            }
        }
    }
    
    /**
     * Pull latest feed from all connected peers
     */
    fun pullFeed() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            
            p2pMessaging.pullFeed().collect { update ->
                when (update) {
                    is FeedUpdate.RefreshStarted -> {
                        // Already set isRefreshing above
                    }
                    is FeedUpdate.PeerUpdated -> {
                        // Feed will auto-update via the feedItems flow
                    }
                    is FeedUpdate.PeerError -> {
                        // Log error but don't stop refresh
                    }
                    is FeedUpdate.RefreshComplete -> {
                        _uiState.update { it.copy(isRefreshing = false) }
                    }
                }
            }
        }
    }
    
    /**
     * Update message input
     */
    fun updateMessageInput(input: String) {
        _uiState.update { it.copy(messageInput = input) }
    }
    
    /**
     * Clear error
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * UI State
 */
data class P2PFeedUiState(
    val messageInput: String = "",
    val isPosting: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)
