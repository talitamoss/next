package com.domain.app.social.ui.feed

import androidx.lifecycle.ViewModel
import com.domain.app.social.contracts.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel for social feed
 * TODO: Cashka will implement this
 */
@HiltViewModel
class SocialFeedViewModel @Inject constructor(
    private val socialRepository: SocialRepository
) : ViewModel() {
    // TODO: Implement feed logic
}
