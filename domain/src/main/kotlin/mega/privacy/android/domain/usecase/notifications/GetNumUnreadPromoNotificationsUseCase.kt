package mega.privacy.android.domain.usecase.notifications

import javax.inject.Inject

/**
 * Use case for getting the number of unread promo notifications.
 */
class GetNumUnreadPromoNotificationsUseCase @Inject constructor(
    private val getEnabledNotificationsUseCase: GetEnabledNotificationsUseCase,
    private val getLastReadNotificationIdUseCase: GetLastReadNotificationIdUseCase,
) {
    /**
     * Invoke.
     *
     * @return Number of unread promo notifications.
     */
    suspend operator fun invoke(): Int {
        val lastReadNotificationId = getLastReadNotificationIdUseCase()
        val promoNotifications = getEnabledNotificationsUseCase()

        return promoNotifications.count { it > lastReadNotificationId }
    }
}