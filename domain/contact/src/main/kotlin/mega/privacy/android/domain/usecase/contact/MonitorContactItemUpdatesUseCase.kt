package mega.privacy.android.domain.usecase.contact

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.scan
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.repository.ContactsRepository
import javax.inject.Inject

/**
 * Monitor a contact, emitting the given initial value first and then the contact with every
 * global contact update applied on top of the latest emitted value.
 */
class MonitorContactItemUpdatesUseCase @Inject constructor(
    private val contactsRepository: ContactsRepository,
    private val applyContactUpdatesUseCase: ApplyContactUpdatesUseCase,
) {
    /**
     * Invoke.
     *
     * @param initial The already resolved contact to monitor.
     * @return Flow of [ContactItem] starting with [initial].
     */
    operator fun invoke(initial: ContactItem): Flow<ContactItem> =
        contactsRepository.monitorContactUpdates()
            .scan(initial) { contact, update -> applyContactUpdatesUseCase(contact, update) }
}
