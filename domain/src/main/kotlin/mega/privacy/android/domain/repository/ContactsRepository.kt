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
     * Requests last green of a user.
     *
     * @param userHandle User handle.
     */
    suspend fun requestLastGreen(userHandle: Long)
}