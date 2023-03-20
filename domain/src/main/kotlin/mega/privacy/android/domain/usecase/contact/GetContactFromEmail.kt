package mega.privacy.android.domain.usecase.contact

import mega.privacy.android.domain.entity.contacts.ContactItem

/**
 * Get contact information from user email
 */
fun interface GetContactFromEmail {

    /**
     * invoke
     *
     * @param email user email
     * @param skipCache If true, force read from backend, refresh cache and return.
     *                  If false, use value in cache
     * @return [ContactItem] which contains contact information of selected user
     */
    suspend operator fun invoke(email: String, skipCache: Boolean): ContactItem?
}