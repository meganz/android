package mega.privacy.android.feature.sync.domain.usecase.notifcation

import mega.privacy.android.feature.sync.domain.entity.SyncNotificationMessage
import javax.inject.Inject

/**
 * Use case to create a unique id for a notification
 */
class CreateSyncNotificationIdUseCase @Inject constructor() {

    /**
     * Creates an ID stable for the notification identity so concurrent collectors update the same
     * platform notification instead of creating duplicates.
     */
    operator fun invoke(notification: SyncNotificationMessage): Int {
        val identity = buildString {
            append(notification.syncNotificationType.name)
            append(':')
            append(notification.notificationDetails.issueId)
            append(':')
            append(notification.notificationDetails.path)
        }
        val candidate = identity.hashCode() and Int.MAX_VALUE
        return when (candidate) {
            0 -> 1
            SYNC_FOREGROUND_NOTIFICATION_ID -> candidate + 1
            else -> candidate
        }
    }

    companion object {
        /**
         * Notification ID reserved for the sync foreground service.
         *
         * Owned here rather than by the worker so that ID allocation and the one ID it must never
         * collide with have a single source of truth.
         */
        const val SYNC_FOREGROUND_NOTIFICATION_ID = 123456
    }
}
