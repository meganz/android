package mega.privacy.android.domain.usecase.contact

import mega.privacy.android.domain.repository.ContactsRepository
import javax.inject.Inject

/**
 * Use case to get a contact from cache by its handle
 *
 * @property contactsRepository [ContactsRepository]
 */
class MonitorContactByHandleUseCase @Inject constructor(
    private val contactsRepository: ContactsRepository,
) {

    /**
     * Invocation method.
     *
     * @param contactId The ID of the contact.
     */
    operator fun invoke(contactId: Long) =
        contactsRepository.monitorContactByHandle(contactId)
}
