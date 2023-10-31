package mega.privacy.android.domain.usecase.contact

import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import mega.privacy.android.domain.repository.ContactsRepository
import javax.inject.Inject

/**
 * Monitor has any contact use case
 *
 * @property contactsRepository [ContactsRepository].
 */
class MonitorHasAnyContactUseCase @Inject constructor(
    private val contactsRepository: ContactsRepository,
) {

    /**
     * Invoke
     *
     * @return Flow of Boolean.
     */
    operator fun invoke() = flow {
        emit(contactsRepository.hasAnyContact())

        merge(
            contactsRepository.monitorContactRemoved(),
            contactsRepository.monitorNewContacts()
        ).collect {
            emit(contactsRepository.hasAnyContact())
        }
    }
}