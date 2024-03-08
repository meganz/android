package mega.privacy.android.domain.usecase.notifications

import mega.privacy.android.domain.repository.NotificationsRepository
import javax.inject.Inject

/**
 * Set last read notification
 */
class SetLastReadNotificationUseCase @Inject constructor(
    private val notificationsRepository: NotificationsRepository,
) {
    /**
     * Invoke
     *
     * @param notificationId ID of the notification to be set as last read. Value `0` is an invalid ID.
     * Passing `0` will clear a previously set last read value.
     */
    suspend operator fun invoke(notificationId: Long) =
        notificationsRepository.setLastReadNotification(notificationId)
}