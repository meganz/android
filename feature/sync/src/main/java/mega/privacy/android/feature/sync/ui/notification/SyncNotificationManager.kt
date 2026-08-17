package mega.privacy.android.feature.sync.ui.notification

import android.app.Notification
import android.annotation.SuppressLint
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationMessage
import mega.privacy.android.feature.sync.domain.usecase.notifcation.CreateSyncNotificationIdUseCase
import mega.privacy.android.shared.sync.ui.permissions.SyncPermissionsManager
import timber.log.Timber
import javax.inject.Inject

/**
 * A class responsible for showing and canceling sync notifications
 */
class SyncNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationManagerCompat: NotificationManagerCompat,
    private val syncNotificationMapper: SyncNotificationMapper,
    private val createSyncNotificationIdUseCase: CreateSyncNotificationIdUseCase,
    private val syncPermissionsManager: SyncPermissionsManager,
) {

    /**
     * Show a notification with the given [SyncNotificationMessage]
     *
     * @return the notification ID
     */
    @SuppressLint("MissingPermission")
    suspend fun show(notificationMessage: SyncNotificationMessage): Int? {
        if (!syncPermissionsManager.isNotificationsPermissionGranted()) return null

        val notificationId = createSyncNotificationIdUseCase(notificationMessage)
        val notification = syncNotificationMapper(context, notificationMessage)
        return try {
            notificationManagerCompat.notify(notificationId, notification)
            notificationId
        } catch (exception: SecurityException) {
            // The permission can be revoked between the check above and the post.
            Timber.w(exception, "Unable to post sync notification $notificationId")
            null
        }
    }

    /**
     * Cancel a notification with the given ID
     */
    fun cancelNotification(notificationId: Int) {
        notificationManagerCompat.cancel(notificationId)
    }

    /**
     * Creates a notification for the foreground service
     */
    fun createForegroundNotification(): Notification {
        return syncNotificationMapper.createForegroundNotification(context)
    }

    companion object {
        /**
         * Notification channel ID
         */
        const val CHANNEL_ID = "sync_error_notifications"

        /**
         * Notification channel name as displayed in Settings -> Apps -> App info -> App notifications
         */
        const val CHANNEL_NAME = "Sync errors / stalled issues"

        /**
         * Notification channel ID for sync progress
         */
        const val SYNC_PROGRESS_CHANNEL_ID = "sync_progress_notification"

        /**
         * Notification channel name for sync progress
         */
        const val SYNC_PROGRESS_CHANNEL_NAME = "Sync Progress"
    }
}
