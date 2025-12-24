package mega.privacy.android.feature.sync.ui.notification

import android.Manifest
import android.app.Notification
import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationManagerCompat
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationMessage
import mega.privacy.android.feature.sync.domain.usecase.notifcation.CreateSyncNotificationIdUseCase
import mega.privacy.android.feature_flags.AppFeatures
import javax.inject.Inject

/**
 * A class responsible for showing and canceling sync notifications
 */
class SyncNotificationManager @Inject constructor(
    private val notificationManagerCompat: NotificationManagerCompat,
    private val syncNotificationMapper: SyncNotificationMapper,
    private val createSyncNotificationIdUseCase: CreateSyncNotificationIdUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
) {

    /**
     * Show a notification with the given [SyncNotificationMessage]
     *
     * @return the notification ID
     */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    suspend fun show(context: Context, notificationMessage: SyncNotificationMessage): Int {
        val notificationId = createSyncNotificationIdUseCase()
        val singleActivity = getFeatureFlagValueUseCase(AppFeatures.SingleActivity)
        val notification = syncNotificationMapper(context, notificationMessage, singleActivity)
        notificationManagerCompat.notify(notificationId, notification)
        return notificationId
    }

    /**
     * Cancel a notification with the given ID
     */
    fun cancelNotification(notificationId: Int) {
        notificationManagerCompat.cancel(notificationId)
    }

    /**
     * Check if the sync notification is currently displayed
     */
    fun isSyncNotificationDisplayed(): Boolean {
        notificationManagerCompat.activeNotifications.forEach { notification ->
            if (notification.notification.channelId == CHANNEL_ID) {
                return true
            }
        }
        return false
    }

    /**
     * Creates a notification for the foreground service
     */
    fun createForegroundNotification(context: Context): Notification {
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
