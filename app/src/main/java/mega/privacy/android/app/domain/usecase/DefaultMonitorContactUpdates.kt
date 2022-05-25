package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.domain.repository.ContactsRepository
import nz.mega.sdk.MegaContactRequest
import javax.inject.Inject

/**
 * Default [MonitorContactUpdates] implementation
 *
 * @property contactsRepository
 */
class DefaultMonitorContactUpdates @Inject constructor(
    private val contactsRepository: ContactsRepository
) : MonitorContactUpdates {
    override fun invoke(): Flow<List<MegaContactRequest>> = contactsRepository.monitorContactRequestUpdates()
}