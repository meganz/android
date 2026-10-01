package mega.privacy.android.domain.usecase.notifications

import mega.privacy.android.domain.entity.notifications.PromoNotification
import mega.privacy.android.domain.repository.NotificationsRepository
import javax.inject.Inject

/**
 * Get list of Promo Notifications
 */
class GetPromoNotificationsUseCase @Inject constructor(
    private val notificationsRepository: NotificationsRepository,
    private val getEnabledNotificationsUseCase: GetEnabledNotificationsUseCase,
    private val getLastReadNotificationUseCase: GetLastReadNotificationIdUseCase,

    ) {
    /**
     * Invoke
     *
     * @return [List<PromoNotification>]
     */
    suspend operator fun invoke(): List<PromoNotification> {
        val enabledID = getEnabledNotificationsUseCase()
        val lastReadNotificationId = getLastReadNotificationUseCase()
        return notificationsRepository.getPromoNotifications().mapNotNull { promoNotification ->
            if (enabledID.contains(promoNotification.promoID.toInt())) {
                promoNotification.copy(isNew = promoNotification.promoID > lastReadNotificationId)
            } else {
                null
            }
        }.sortedByDescending { it.promoID }
    }
}