package mega.privacy.android.domain.usecase.account.contactrequest

import mega.privacy.android.domain.repository.ContactsRepository
import javax.inject.Inject

/**
 * Monitor global contact request updates for the current logged in user
 */
class MonitorContactRequestUpdatesUseCase @Inject constructor(
    private val contactsRepository: ContactsRepository,
) {
    /**
     * Invoke
     *
     * @return a flow of changes
     */
    operator fun invoke() = contactsRepository.monitorContactRequestUpdates()
}