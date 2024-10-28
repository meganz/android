package mega.privacy.android.domain.usecase.requeststatus

import mega.privacy.android.domain.repository.NotificationsRepository
import javax.inject.Inject

/**
 * Use case to enable request status monitor to receive EVENT_REQSTAT_PROGRESS events
 */
class EnableRequestStatusMonitorUseCase @Inject constructor(
    private val notificationsRepository: NotificationsRepository,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke() = notificationsRepository.enableRequestStatusMonitor()
}