package mega.privacy.android.domain.usecase.notifications

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import mega.privacy.android.domain.repository.NotificationsRepository
import javax.inject.Inject

/**
 * Monitor unread user alerts count use-case
 *
 * @property notificationsRepository
 */
class MonitorUnreadAlertsCountUseCase @Inject constructor(
    private val notificationsRepository: NotificationsRepository,
) {
    /**
     * Invoke
     */
    operator fun invoke() = notificationsRepository.monitorUserAlerts().onStart {
        emit(notificationsRepository.getUserAlerts())
    }.map { userAlerts ->
        userAlerts.count { !it.seen }
    }
}