package com.domain.app.social.contracts

data class SharingPermissions(
    val canReshare: Boolean = false,
    val expiresAt: Long? = null
)
