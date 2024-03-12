package mega.privacy.android.app.presentation.notification.model

import mega.privacy.android.domain.entity.notifications.PromoNotification

/**
 * Notification state
 *
 * @property notifications
 * @property promoNotifications
 * @property scrollToTop
 */
data class NotificationState(
    val notifications: List<Notification> = emptyList(),
    val promoNotifications: List<PromoNotification> = emptyList(),
    val scrollToTop: Boolean = false,
)
