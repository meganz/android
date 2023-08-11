package mega.privacy.android.app.fcm

import android.app.NotificationManager
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import mega.privacy.android.app.utils.Constants
import javax.inject.Inject

/**
 * Use case to create the required transfer Notification Channels
 *
 * @property notificationManager
 */
internal class CreateTransferNotificationChannelsUseCase @Inject constructor(
    private val notificationManager: NotificationManagerCompat,
) {
    /**
     * Create the required transfer notification Channels
     */
    operator fun invoke() {
        val newChannels = listOf(
            NotificationChannelCompat.Builder(
                NOTIFICATION_CHANNEL_DOWNLOAD_ID,
                NotificationManager.IMPORTANCE_DEFAULT
            )
                .setName(NOTIFICATION_CHANNEL_DOWNLOAD_NAME)
                .setShowBadge(false)
                .setSound(null, null)
                .build(),
            NotificationChannelCompat.Builder(
                NOTIFICATION_CHANNEL_UPLOAD_ID,
                NotificationManager.IMPORTANCE_DEFAULT
            )
                .setName(NOTIFICATION_CHANNEL_UPLOAD_NAME)
                .setShowBadge(false)
                .setSound(null, null)
                .build(),
        )
        notificationManager.createNotificationChannelsCompat(newChannels)

    }

    companion object {
        const val NOTIFICATION_CHANNEL_DOWNLOAD_ID =
            Constants.NOTIFICATION_CHANNEL_DOWNLOAD_ID
        internal const val NOTIFICATION_CHANNEL_DOWNLOAD_NAME =
            Constants.NOTIFICATION_CHANNEL_DOWNLOAD_NAME
        const val NOTIFICATION_CHANNEL_UPLOAD_ID =
            Constants.NOTIFICATION_CHANNEL_UPLOAD_ID
        internal const val NOTIFICATION_CHANNEL_UPLOAD_NAME =
            Constants.NOTIFICATION_CHANNEL_UPLOAD_NAME
    }
}