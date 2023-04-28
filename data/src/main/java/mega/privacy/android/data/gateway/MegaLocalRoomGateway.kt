package mega.privacy.android.data.gateway

import mega.privacy.android.domain.entity.Contact

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
    suspend fun saveContact(contact: Contact)

    /**
     * Set contact name
     *
     * @param firstName
     * @param mail
     * @return
     */
    suspend fun setContactName(firstName: String?, mail: String?)

    /**
     * Set contact last name
     *
     * @param lastName
     * @param mail
     */
    suspend fun setContactLastName(lastName: String?, mail: String?)

    /**
     * Set contact mail
     *
     * @param handle
     * @param mail
     */
    suspend fun setContactMail(handle: Long, mail: String?)

    /**
     * Set contact fist name
     *
     * @param handle
     * @param firstName
     */
    suspend fun setContactFistName(handle: Long, firstName: String?)

    /**
     * Set contact last name
     *
     * @param handle
     * @param lastName
     */
    suspend fun setContactLastName(handle: Long, lastName: String?)

    /**
     * Set contact nickname
     *
     * @param handle
     * @param nickname
     */
    suspend fun setContactNickname(handle: Long, nickname: String?)

    /**
     * Find contact by handle
     *
     * @param handle
     * @return
     */
    suspend fun findContactByHandle(handle: Long): Contact?

    /**
     * Find contact by email
     *
     * @param mail
     * @return
     */
    suspend fun findContactByEmail(mail: String?): Contact?

    /**
     * Clear contacts
     *
     */
    suspend fun clearContacts()

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
}