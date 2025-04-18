package mega.privacy.android.feature.sync.ui

import mega.privacy.android.feature.sync.domain.entity.SyncNotificationMessage
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationType

/**
 * State for the [SyncMonitorViewModel].
 *
 * @property syncNotificationType The type of sync notification.
 * @property displayNotification Event to display a push notification.
 */
data class SyncMonitorState(
    val syncNotificationType: SyncNotificationType? = null,
    val displayNotification: SyncNotificationMessage? = null,
)
