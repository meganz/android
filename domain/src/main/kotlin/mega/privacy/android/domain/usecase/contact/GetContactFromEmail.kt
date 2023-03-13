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
     * @return [ContactItem] which contains contact information of selected user
     */
    suspend operator fun invoke(email: String): ContactItem?
}