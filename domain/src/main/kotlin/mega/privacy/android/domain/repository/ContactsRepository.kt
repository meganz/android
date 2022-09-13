package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.contacts.ContactRequest
import mega.privacy.android.domain.entity.user.UserUpdate

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
     * Monitor contact updates
     *
     * @return A flow of all global contact updates.
     */
    fun monitorContactUpdates(): Flow<UserUpdate>
}