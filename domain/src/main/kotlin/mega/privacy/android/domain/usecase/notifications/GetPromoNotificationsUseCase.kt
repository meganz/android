package mega.privacy.android.domain.usecase.notifications

import mega.privacy.android.domain.entity.notifications.PromoNotification
import mega.privacy.android.domain.repository.NotificationsRepository
import javax.inject.Inject

/**
 * Get list of Promo Notifications
 */
class GetPromoNotificationsUseCase @Inject constructor(
    private val notificationsRepository: NotificationsRepository,
) {
    /**
     * Invoke
     *
     * @return [List<PromoNotification>]
     */
    suspend operator fun invoke(): List<PromoNotification> =
        notificationsRepository.getPromoNotifications()
}