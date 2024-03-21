package mega.privacy.android.data.gateway.contact

import mega.privacy.android.domain.entity.contacts.LocalContact

/**
 * User's contacts related gateway
 */
interface ContactGateway {

    /**
     * Get list of local contacts
     *
     * @return List of [LocalContact]
     */
    suspend fun getLocalContacts(): List<LocalContact>

    /**
     * Get list of local contact's numbers
     *
     * @return List of [LocalContact]
     */
    suspend fun getLocalContactNumbers(): List<LocalContact>

    /**
     * Get list of local contact's email addresses
     *
     * @return List of [LocalContact]
     */
    suspend fun getLocalContactEmailAddresses(): List<LocalContact>
}
