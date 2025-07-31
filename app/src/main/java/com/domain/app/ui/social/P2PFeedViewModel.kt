package com.domain.app.ui.social
import com.domain.app.social.contracts.*
import java.time.Instant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domain.app.social.contracts.FeedItem
import com.domain.app.network.P2PNetworkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for P2P Feed Screen
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
        // Observe feed items
        viewModelScope.launch {
            networkManager.feedItems.collect { items ->
                _uiState.value = _uiState.value.copy(
                    feedItems = items,
                    isLoading = false
                )
            }
        }
        
        // Initial refresh
        refreshFeed()
    }
    
    fun refreshFeed() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                networkManager.pullFeedFromPeers()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }
}

data class P2PFeedUiState(
    val feedItems: List<FeedItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

    private fun convertToUIFeedItem(protocolItem: com.domain.app.network.protocol.FeedItem): FeedItem {
        return FeedItem(
            id = protocolItem.id,
            authorId = "peer_${protocolItem.contentId}",
            authorName = "Anonymous Peer",
            contentType = ContentType.DATA_INSIGHT,
            content = FeedContent.DataInsight(
                title = protocolItem.title ?: "Shared Data",
                description = protocolItem.preview ?: "",
                value = "",
                unit = null,
                emoji = "📊",
                pluginId = protocolItem.contentType
            ),
            timestamp = Instant.ofEpochMilli(protocolItem.timestamp),
            privacyLevel = PrivacyLevel.FRIENDS
        )
    }
