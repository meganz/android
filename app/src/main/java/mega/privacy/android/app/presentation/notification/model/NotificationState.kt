package mega.privacy.android.app.presentation.notification.model

/**
 * Notification state
 *
 * @property notifications
 * @property scrollToTop
 */
data class NotificationState(
    val notifications: List<Notification>,
    val scrollToTop: Boolean = false,
)
