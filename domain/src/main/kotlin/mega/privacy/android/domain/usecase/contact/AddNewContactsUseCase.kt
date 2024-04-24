package mega.privacy.android.domain.usecase.contact

import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.ContactRequest
import mega.privacy.android.domain.repository.ContactsRepository
import javax.inject.Inject

/**
 * A use case to add new contacts on a [ContactItem] list.
 */
class AddNewContactsUseCase @Inject constructor(
    private val contactsRepository: ContactsRepository
) {

    /**
     * Invocation method.
     *
     * @param outdatedContactList [ContactItem] list to update.
     * @param newContacts         List of new contacts.
     *
     * @return The updated [ContactItem] list.
     */
    suspend operator fun invoke(
        outdatedContactList: List<ContactItem>,
        newContacts: List<ContactRequest>,
    ): List<ContactItem> = contactsRepository.addNewContacts(
        outdatedContactList = outdatedContactList,
        newContacts = newContacts
    )
}
