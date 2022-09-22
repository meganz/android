package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.ContactRequest
import mega.privacy.android.domain.entity.contacts.OnlineStatus
import mega.privacy.android.domain.entity.user.UserLastGreen
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
     * Monitor updates on last green.
     *
     * @return A flow of [UserLastGreen]
     */
    fun monitorChatPresenceLastGreenUpdates(): Flow<UserLastGreen>

    /**
     * Requests last green of a user.
     *
     * @param userHandle User handle.
     */
    suspend fun requestLastGreen(userHandle: Long)

    /**
     * Monitor contact updates
     *
     * @return A flow of all global contact updates.
     */
    fun monitorContactUpdates(): Flow<UserUpdate>

    /**
     * Starts a chat conversation with the provided contacts.
     *
     * @param isGroup     True if is should create a group chat, false otherwise.
     * @param userHandles List of contact handles.
     * @return The chat conversation handle.
     */
    suspend fun startConversation(isGroup: Boolean, userHandles: List<Long>): Long

    /**
     * Set open invite.
     *
     * @param chatId
     * @param enabled
     * @return The chat conversation handle.
     */
    suspend fun setOpenInvite(chatId: Long, enabled: Boolean): Long

    /**
     * Monitor updates on chat online statuses.
     *
     * @return A flow of [OnlineStatus].
     */
    fun monitorChatOnlineStatusUpdates(): Flow<OnlineStatus>

    /**
     * Gets visible contacts.
     *
     * @return A list with all visible contacts.
     */
    suspend fun getVisibleContacts(): List<ContactItem>

    /**
     * Gets the updated main data of a contact.
     *
     * @param contactItem [ContactItem] whose data is going to be requested.
     * @return [ContactData] containing the updated data.
     */
    suspend fun getContactData(contactItem: ContactItem): ContactData

    /**
     * Updates the contact list with the received contact updates.
     *
     * @param outdatedContactList Outdated contact list.
     * @param contactUpdates      Map with all contact updates.
     * @return The updated contact list.
     */
    suspend fun applyContactUpdates(
        outdatedContactList: List<ContactItem>,
        contactUpdates: UserUpdate,
    ): List<ContactItem>

    /**
     * Updates the contact list with the new contact.
     *
     * @param outdatedContactList Outdated contact list.
     * @param newContacts         List with new contacts.
     * @return The updated contact list.
     */
    suspend fun addNewContacts(
        outdatedContactList: List<ContactItem>,
        newContacts: List<ContactRequest>,
    ): List<ContactItem>
}