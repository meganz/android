package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.NotificationsRepository
import javax.inject.Inject

/**
 * Acknowledge user alerts use case
 *
 * @property repository Notifications repository
 */
class AcknowledgeUserAlertsUseCase @Inject constructor(
    private val repository: NotificationsRepository
) {
    /**
     * Invoke
     */
    suspend operator fun invoke() = repository.acknowledgeUserAlerts()
}