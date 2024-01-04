package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.ContactsRepository
import javax.inject.Inject

/**
 * Monitor Contact Cache Updates
 *
 */
class MonitorContactCacheUpdates @Inject constructor(
    private val repository: ContactsRepository,
) {
    /**
     * Invoke
     *
     */
    operator fun invoke() = repository.monitorContactCacheUpdates
}