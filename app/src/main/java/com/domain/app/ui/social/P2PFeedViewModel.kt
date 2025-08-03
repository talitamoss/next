package com.domain.app.ui.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domain.app.network.P2PNetworkManager
import com.domain.app.social.contracts.FeedItem
import com.domain.app.social.contracts.toDomainModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for P2P Feed screen
 * Manages social feed data and interactions
 * 
 * File location: app/src/main/java/com/domain/app/ui/social/P2PFeedViewModel.kt
 */
@HiltViewModel
class P2PFeedViewModel @Inject constructor(
    private val networkManager: P2PNetworkManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(P2PFeedUiState())
    val uiState: StateFlow<P2PFeedUiState> = _uiState.asStateFlow()
    
    init {
        loadFeed()
        observeNetworkStatus()
    }
    
    /**
     * Load feed items from network
     */
    private fun loadFeed() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // Get feed items from network
                val networkFeedItems = networkManager.getFeedItems()
                
                // Convert network protocol items to domain model
                val feedItems = networkFeedItems.map { networkItem ->
                    // Get author name from contacts or use ID as fallback
                    val authorName = networkManager.getContactName(networkItem.authorId) 
                        ?: "User ${networkItem.authorId.take(8)}"
                    
                    networkItem.toDomainModel(authorName)
                }
                
                _uiState.update { 
                    it.copy(
                        feedItems = feedItems,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        error = "Failed to load feed: ${e.message}",
                        isLoading = false
                    )
                }
            }
        }
    }
    
    /**
     * Observe network connection status
     */
    private fun observeNetworkStatus() {
        viewModelScope.launch {
            networkManager.connectionStatus.collect { status ->
                _uiState.update { 
                    it.copy(
                        isConnected = status.isConnected,
                        connectedPeers = status.connectedPeers
                    )
                }
            }
        }
    }
    
    /**
     * Post a new feed item
     */
    fun postItem(content: String, type: com.domain.app.social.contracts.FeedItemType) {
        viewModelScope.launch {
            _uiState.update { it.copy(isPosting = true) }
            
            try {
                val success = networkManager.postFeedItem(content, type.name)
                
                if (success) {
                    // Reload feed to show new item
                    loadFeed()
                    _uiState.update { it.copy(isPosting = false) }
                } else {
                    throw Exception("Failed to post item")
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        error = "Failed to post: ${e.message}",
                        isPosting = false
                    )
                }
            }
        }
    }
    
    /**
     * Refresh feed
     */
    fun refresh() {
        loadFeed()
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * P2P Feed UI state
 */
data class P2PFeedUiState(
    val feedItems: List<FeedItem> = emptyList(),
    val isLoading: Boolean = false,
    val isPosting: Boolean = false,
    val isConnected: Boolean = false,
    val connectedPeers: Int = 0,
    val error: String? = null
)
