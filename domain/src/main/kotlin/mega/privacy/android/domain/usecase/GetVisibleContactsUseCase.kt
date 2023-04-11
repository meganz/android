package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.repository.ContactsRepository
import javax.inject.Inject

/**
 * Use case for getting visible contacts with the cached data, not the updated one.
 * For getting the updated date see [GetContactData].
 */
class GetVisibleContactsUseCase @Inject constructor(
    private val contactsRepository: ContactsRepository
) {

    /**
     * Invoke.
     *
     * @return A flow of list with all visible contacts.
     */
    suspend operator fun invoke(): List<ContactItem> = contactsRepository.getVisibleContacts()
}