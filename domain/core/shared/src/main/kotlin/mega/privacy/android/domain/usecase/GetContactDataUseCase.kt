package mega.privacy.android.domain.usecase


import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.repository.ContactsRepository
import javax.inject.Inject

/**
 * Use case for getting the updated main data of a contact.
 */
class GetContactDataUseCase @Inject constructor(
    private val contactsRepository: ContactsRepository,
) {

    /**
     * Invoke.
     *
     * @param contactItem [ContactItem] whose data is going to be requested.
     * @return [ContactData] containing the updated data.
     */
    suspend operator fun invoke(contactItem: ContactItem): ContactData =
        contactsRepository.getContactData(contactItem)
}