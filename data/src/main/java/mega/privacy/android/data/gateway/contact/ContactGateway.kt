package mega.privacy.android.data.gateway.contact

import mega.privacy.android.domain.entity.contacts.LocalContact
import mega.privacy.android.domain.entity.uri.UriPath

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
     * Get list of local contacts from a contact picker session [UriPath].
     *
     * The [UriPath] is returned by the Android system contact picker and can be queried
     * without the READ_CONTACTS permission. Only contacts with email addresses are returned,
     * grouped per contact.
     *
     * @param uriPath The [UriPath] returned by the contact picker.
     * @return List of [LocalContact]
     */
    suspend fun getLocalContactsFromUri(uriPath: UriPath): List<LocalContact>

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
