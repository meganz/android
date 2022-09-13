package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.contacts.ContactRequest

/**
 * Contacts repository.
 */
interface ContactsRepository {

    /**
     * Monitor contact request updates.
     *
     * @return A flow of all global contact request updates.
     */
    fun monitorContactRequestUpdates(): Flow<List<ContactRequest>>

    /**
     * Starts a chat conversation with the provided contacts.
     *
     * @param isGroup     True if is should create a group chat, false otherwise.
     * @param userHandles List of contact handles.
     * @return The chat conversation handle.
     */
    suspend fun startConversation(isGroup: Boolean, userHandles: List<Long>): Long
}