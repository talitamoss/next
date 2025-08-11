// com.domain.app.social.ui.feed.SocialFeedViewModel.kt

package com.domain.app.social.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domain.app.social.contracts.FeedItem
import com.domain.app.social.contracts.SocialFeedRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SocialFeedViewModel @Inject constructor(
    private val repository: SocialFeedRepository
) : ViewModel() {

    private val _feedItems = MutableStateFlow<List<FeedItem>>(emptyList())
    val feedItems: StateFlow<List<FeedItem>> = _feedItems

    init {
        viewModelScope.launch {
            _feedItems.value = repository.getFeedItems()
        }
    }
}
