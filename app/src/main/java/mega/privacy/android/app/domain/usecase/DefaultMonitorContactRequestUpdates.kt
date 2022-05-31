package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.domain.repository.ContactsRepository
import nz.mega.sdk.MegaContactRequest
import javax.inject.Inject

/**
 * Default [MonitorContactRequestUpdates] implementation
 *
 * @property contactsRepository [ContactsRepository]
 */
class DefaultMonitorContactRequestUpdates @Inject constructor(
    private val contactsRepository: ContactsRepository
) : MonitorContactRequestUpdates {
    override fun invoke(): Flow<List<MegaContactRequest>> = contactsRepository.monitorContactRequestUpdates()
}