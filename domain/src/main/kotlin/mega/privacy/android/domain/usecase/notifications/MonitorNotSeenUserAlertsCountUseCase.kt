package mega.privacy.android.domain.usecase.notifications

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import mega.privacy.android.domain.repository.NotificationsRepository
import javax.inject.Inject

/**
 * Monitor not seen user alerts count use-case
 *
 * @property notificationsRepository
 */
class MonitorNotSeenUserAlertsCountUseCase @Inject constructor(
    private val notificationsRepository: NotificationsRepository,
) {
    /**
     * Invoke
     */
    operator fun invoke() = notificationsRepository.monitorNotSeenUserAlerts().onStart {
        emit(notificationsRepository.getNotSeenUserAlerts())
    }.map { it.size }
}