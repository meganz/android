package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.ContactRequest

/**
 * Add new contacts on a [ContactItem] list.
 */
fun interface AddNewContacts {

    /**
     * Invoke.
     *
     * @param outdatedContactList [ContactItem] list to update.
     * @param newContacts         List of new contacts.
     * @return The updated [ContactItem] list.
     */
    suspend operator fun invoke(
        outdatedContactList: List<ContactItem>,
        newContacts: List<ContactRequest>,
    ): List<ContactItem>
}