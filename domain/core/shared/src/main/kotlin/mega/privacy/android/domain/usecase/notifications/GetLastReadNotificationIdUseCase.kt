package mega.privacy.android.domain.usecase.notifications

import mega.privacy.android.domain.repository.NotificationsRepository
import javax.inject.Inject

/**
 * Get last read notification ID
 */
class GetLastReadNotificationIdUseCase @Inject constructor(
    private val notificationsRepository: NotificationsRepository,
) {
    /**
     * Invoke
     *
     * @return [Long]
     */
    suspend operator fun invoke(): Long =
        notificationsRepository.getLastReadNotificationId()
}