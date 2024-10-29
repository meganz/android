package mega.privacy.android.feature.sync.ui

import mega.privacy.android.feature.sync.domain.entity.SyncNotificationMessage

/**
 * State for the [SyncMonitorViewModel].
 *
 * @property displayNotification Event to display a push notification.
 */
data class SyncMonitorState(
    val displayNotification: SyncNotificationMessage? = null,
)