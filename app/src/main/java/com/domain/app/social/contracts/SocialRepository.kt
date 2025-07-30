package com.domain.app.social.contracts

import kotlinx.coroutines.flow.Flow

interface SocialRepository {

    // Contacts
    fun getContacts(): Flow<List<SocialContact>>
    suspend fun addContact(contactCode: String): Result<SocialContact>
    suspend fun removeContact(contactId: String): Result<Unit>

    // Feed
    fun getFeed(): Flow<List<FeedItem>>
    suspend fun refreshFeed(): Result<Unit>
    suspend fun markAsRead(itemId: String)

    // Sharing
    suspend fun shareContent(dataPointId: String, permissions: SharingPermissions): Result<Unit>
    fun getShareableContent(): Flow<List<ShareableDataPoint>>
}

