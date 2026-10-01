package mega.privacy.android.domain.usecase.contact

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.Contact
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
     * @return A [Flow] of the cached [Contact], emitting null while the contact is not cached.
     */
    operator fun invoke(contactId: Long): Flow<Contact?> =
        contactsRepository.monitorContactByHandle(contactId)
}
