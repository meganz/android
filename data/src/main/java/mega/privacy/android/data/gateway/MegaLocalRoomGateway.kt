package mega.privacy.android.data.gateway

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.Contact
import mega.privacy.android.domain.entity.transfer.CompletedTransfer

/**
 * Mega local room gateway
 *
 */
interface MegaLocalRoomGateway {
    /**
     * Save contact
     *
     * @param contact
     */
    suspend fun insertContact(contact: Contact)

    /**
     * Set contact name
     *
     * @param firstName
     * @param email
     * @return
     */
    suspend fun updateContactNameByEmail(firstName: String?, email: String?)

    /**
     * Set contact last name
     *
     * @param lastName
     * @param email
     */
    suspend fun updateContactLastNameByEmail(lastName: String?, email: String?)

    /**
     * Set contact mail
     *
     * @param handle
     * @param email
     */
    suspend fun updateContactMailByHandle(handle: Long, email: String?)

    /**
     * Set contact fist name
     *
     * @param handle
     * @param firstName
     */
    suspend fun updateContactFistNameByHandle(handle: Long, firstName: String?)

    /**
     * Set contact last name
     *
     * @param handle
     * @param lastName
     */
    suspend fun updateContactLastNameByHandle(handle: Long, lastName: String?)

    /**
     * Set contact nickname
     *
     * @param handle
     * @param nickname
     */
    suspend fun updateContactNicknameByHandle(handle: Long, nickname: String?)

    /**
     * Find contact by handle
     *
     * @param handle
     * @return
     */
    suspend fun getContactByHandle(handle: Long): Contact?

    /**
     * Find contact by email
     *
     * @param email
     * @return
     */
    suspend fun getContactByEmail(email: String?): Contact?

    /**
     * Clear contacts
     *
     */
    suspend fun deleteAllContacts()

    /**
     * Get contact count
     *
     * @return
     */
    suspend fun getContactCount(): Int

    /**
     * Get all contacts
     *
     */
    suspend fun getAllContacts(): List<Contact>

    /**
     * Get all completed transfers
     *
     * @param size the limit size of the list. If null, the limit does not apply
     */
    suspend fun getAllCompletedTransfers(size: Int? = null): Flow<List<CompletedTransfer>>

    /**
     * Get the completed transfers count
     */
    suspend fun getCompletedTransfersCount(): Int
}
