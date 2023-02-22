package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.contacts.ContactItem

/**
 * Use case for getting visible contacts with the cached data, not the updated one.
 * For getting the updated date see [GetContactData].
 */
fun interface GetVisibleContacts {

    /**
     * Invoke.
     *
     * @return A flow of list with all visible contacts.
     */
    suspend operator fun invoke(): List<ContactItem>
}