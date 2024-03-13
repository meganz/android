package mega.privacy.android.domain.usecase.notifications

import javax.inject.Inject

/**
 * Use case for getting the number of unread promo notifications.
 */
class GetNumUnreadPromoNotificationsUseCase @Inject constructor(
    private val getPromoNotificationsUseCase: GetPromoNotificationsUseCase,
    private val getLastReadNotificationUseCase: GetLastReadNotificationIDUseCase,
) {
    /**
     * Invoke.
     *
     * @return Number of unread promo notifications.
     */
    suspend operator fun invoke(): Int {
        val promoNotifications = getPromoNotificationsUseCase()
        val lastReadNotificationId = getLastReadNotificationUseCase()

        return promoNotifications.count { it.promoID > lastReadNotificationId }
    }
}