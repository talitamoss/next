package com.domain.app.social.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domain.app.social.contracts.FeedItem
import com.domain.app.social.contracts.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SocialFeedViewModel @Inject constructor(
    private val socialRepository: SocialRepository
) : ViewModel() {

    private val _feedItems = MutableStateFlow<List<FeedItem>>(emptyList())
    val feedItems: StateFlow<List<FeedItem>> = _feedItems.asStateFlow()

    init {
        observeFeed()
    }

    private fun observeFeed() {
        socialRepository.getFeed()
            .onEach { items -> _feedItems.value = items }
            .launchIn(viewModelScope)
    }

    fun refreshFeed() {
        viewModelScope.launch {
            socialRepository.refreshFeed()
        }
    }

    fun markItemAsRead(itemId: String) {
        viewModelScope.launch {
            socialRepository.markAsRead(itemId)
        }
    }
}
