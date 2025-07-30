package com.domain.app.social.contracts

import java.time.Instant

data class SocialContact(
    val id: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val isOnline: Boolean = false,
    val lastSeen: Instant? = null,
    val trustLevel: TrustLevel = TrustLevel.FRIEND
)

enum class TrustLevel {
    BLOCKED, STRANGER, ACQUAINTANCE, FRIEND, CLOSE_FRIEND, FAMILY
}
