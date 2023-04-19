package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.contacts.AccountCredentials
import mega.privacy.android.domain.entity.chat.ChatConnectionState
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.ContactRequest
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import mega.privacy.android.domain.entity.contacts.OnlineStatus
import mega.privacy.android.domain.entity.contacts.UserStatus
import mega.privacy.android.domain.entity.user.UserId
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
     * Monitor updates on chat online statuses.
     *
     * @return A flow of [OnlineStatus].
     */
    fun monitorChatOnlineStatusUpdates(): Flow<OnlineStatus>

    /**
     * Monitor updates on chat connection state.
     *
     * @return A flow of [ChatConnectionState].
     */
    fun monitorChatConnectionStateUpdates(): Flow<ChatConnectionState>

    /**
     * Gets visible contacts with the cached data, not the updated one.
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
     * Gets the ContactItem for a given [UserId]
     * @param userId of the user we want to fetch
     * @param skipCache if true a new fetch will be done, if false it may return a cached info
     */
    suspend fun getContactItem(userId: UserId, skipCache: Boolean): ContactItem?

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

    /**
     * Get the alias of the given user if any
     *
     * @param handle User identifier.
     * @return User alias.
     */
    suspend fun getUserAlias(handle: Long): String

    /**
     * Get the email of the given user.
     *
     * @param handle    User identifier.
     * @param skipCache Skip cached result.
     * @return User email
     */
    suspend fun getUserEmail(handle: Long, skipCache: Boolean = false): String

    /**
     * Gets the name of the given user: The alias if any, the full name in other case.
     * If not available then returns the email.
     *
     * @param handle User identifier.
     * @param skipCache Skip cached result.
     * @param shouldNotify
     * @return User first name.
     */
    suspend fun getUserFirstName(
        handle: Long,
        skipCache: Boolean = false,
        shouldNotify: Boolean = false,
    ): String

    /**
     * Gets the name of the given user: The alias if any, the full name in other case.
     * If not available then returns the email.
     *
     * @param handle User identifier.
     * @param skipCache Skip cached result.
     * @param shouldNotify
     * @return User last name.
     */
    suspend fun getUserLastName(
        handle: Long,
        skipCache: Boolean = false,
        shouldNotify: Boolean = false,
    ): String

    /**
     * Get the full name of the given user.
     *
     * @param handle User identifier
     * @param skipCache Skip cached result.
     * @return User full name.
     */
    suspend fun getUserFullName(handle: Long, skipCache: Boolean = false): String

    /**
     * Checks if the credentials of a given user are already verified.
     *
     * @param userEmail The contact's email.
     * @return True if credentials are verified, false otherwise.
     */
    suspend fun areCredentialsVerified(userEmail: String): Boolean

    /**
     * Resets the credentials of the given user.
     *
     * @param userEmail The contact's email.
     */
    suspend fun resetCredentials(userEmail: String)

    /**
     * Verifies the credentials of the given user.
     *
     * @param userEmail The contact's email.
     */
    suspend fun verifyCredentials(userEmail: String)

    /**
     * Gets contact's credentials.
     *
     * @param userEmail User's email.
     * @return [AccountCredentials.ContactCredentials]
     */
    suspend fun getContactCredentials(userEmail: String): AccountCredentials.ContactCredentials?

    /**
     * Get current user first name
     *
     * @param forceRefresh If true, force read from backend, refresh cache and return.
     *                     If false, use value in cache
     * @return first name
     */
    suspend fun getCurrentUserFirstName(forceRefresh: Boolean): String

    /**
     * Get current user last name
     *
     * @param forceRefresh If true, force read from backend, refresh cache and return.
     *                     If false, use value in cache
     * @return last name
     */
    suspend fun getCurrentUserLastName(forceRefresh: Boolean): String

    /**
     * Update user first name
     *
     * @param value new user first name
     * @return
     */
    suspend fun updateCurrentUserFirstName(value: String): String

    /**
     * Update user last name
     *
     * @param value new user last name
     * @return
     */
    suspend fun updateCurrentUserLastName(value: String): String

    /**
     * Invite a new contact
     *
     * @param email Email of the new contact
     * @param handle Handle of the contact
     * @param message Message for the user (can be NULL)
     */
    suspend fun inviteContact(email: String, handle: Long, message: String?): InviteContactRequest

    /**
     * Get aliases
     * @return the map of key is user handle and value is user nick name
     */
    suspend fun getCurrentUserAliases(): Map<Long, String>

    /**
     * Get contact emails
     *
     * @return the map of key is handle and value is email
     */
    suspend fun getContactEmails(): Map<Long, String>

    /**
     * Clear contact database
     *
     */
    suspend fun clearContactDatabase()

    /**
     * Save contact
     *
     * @param handle
     * @param email
     * @param firstName
     * @param lastName
     */
    suspend fun saveContact(handle: Long, email: String, firstName: String, lastName: String)

    /**
     * Get contact database size
     *
     * @return the number of record in database
     */
    suspend fun getContactDatabaseSize(): Int

    /**
     * Get contact email and save to local database
     *
     * @param handle user handle id
     */
    suspend fun getContactEmail(handle: Long): String

    /**
     * Get contact email and save to local database
     *
     * @param handle user handle id
     */
    suspend fun getUserOnlineStatusByHandle(handle: Long): UserStatus

    /**
     * Get user email from chat id
     *
     * @param handle user handle
     */
    suspend fun getUserEmailFromChat(handle: Long): String?

    /**
     * Get contact item from user email
     *
     * @param email user email
     * @param skipCache If true, force read from backend, refresh cache and return.
     *                  If false, use value in cache
     * @return [ContactItem]
     */
    suspend fun getContactItemFromUserEmail(email: String, skipCache: Boolean): ContactItem?

    /**
     * Set user alias
     *
     * @param name new nick name
     * @param userHandle user handle
     */
    suspend fun setUserAlias(name: String?, userHandle: Long): String?

    /**
     * Get the alias of the given user if any
     *
     * @param email User email.
     * @return User alias.
     */
    suspend fun getAvatarUri(email: String): String?

    /**
     * Deletes Avatar file from cache if exists
     *
     * @param email email id of the user
     */
    suspend fun deleteAvatar(email: String)

    /**
     * Remove the selected contact from mega account
     *
     * @param email email address of the user
     */
    suspend fun removeContact(email: String): Boolean
}