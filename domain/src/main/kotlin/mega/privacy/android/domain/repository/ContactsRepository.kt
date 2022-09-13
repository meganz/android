package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.contacts.ContactRequest
import mega.privacy.android.domain.entity.user.UserLastGreen

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
     * Monitor updates on last green.
     *
     * @return A flow of [UserLastGreen]
     */
    fun monitorChatPresenceLastGreenUpdates(): Flow<UserLastGreen>
}