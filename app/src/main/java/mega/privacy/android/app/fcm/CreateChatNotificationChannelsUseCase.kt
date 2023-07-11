package mega.privacy.android.app.fcm

import android.app.NotificationManager
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import mega.privacy.android.app.utils.Constants.NOTIFICATION_CHANNEL_CHAT_ID
import mega.privacy.android.app.utils.Constants.NOTIFICATION_CHANNEL_CHAT_NAME
import mega.privacy.android.app.utils.Constants.NOTIFICATION_CHANNEL_CHAT_SUMMARY_ID_V2
import mega.privacy.android.app.utils.Constants.NOTIFICATION_CHANNEL_CHAT_SUMMARY_NAME
import mega.privacy.android.app.utils.Constants.NOTIFICATION_CHANNEL_INCOMING_CALLS_ID
import mega.privacy.android.app.utils.Constants.NOTIFICATION_CHANNEL_INCOMING_CALLS_NAME
import mega.privacy.android.app.utils.Constants.NOTIFICATION_CHANNEL_INPROGRESS_MISSED_CALLS_ID
import mega.privacy.android.app.utils.Constants.NOTIFICATION_CHANNEL_INPROGRESS_MISSED_CALLS_NAME
import javax.inject.Inject

/**
 * Use case to create the required chat Notification Channels
 *
 * @property notificationManager
 */
class CreateChatNotificationChannelsUseCase @Inject constructor(
    private val notificationManager: NotificationManagerCompat,
) {

    /**
     * Create chat channels
     */
    operator fun invoke() {
        val currentChannels = notificationManager.notificationChannelsCompat.map { it.id }
        val newChannels = mutableListOf<NotificationChannelCompat>()

        if (currentChannels.none { it == NOTIFICATION_CHANNEL_CHAT_SUMMARY_ID_V2 }) {
            newChannels.add(
                NotificationChannelCompat.Builder(
                    NOTIFICATION_CHANNEL_CHAT_SUMMARY_ID_V2,
                    NotificationManager.IMPORTANCE_HIGH
                ).setName(NOTIFICATION_CHANNEL_CHAT_SUMMARY_NAME).build()
            )
        }
        if (currentChannels.none { it == NOTIFICATION_CHANNEL_CHAT_ID }) {
            newChannels.add(
                NotificationChannelCompat.Builder(
                    NOTIFICATION_CHANNEL_CHAT_ID,
                    NotificationManager.IMPORTANCE_HIGH
                ).setName(NOTIFICATION_CHANNEL_CHAT_NAME).build()
            )
        }
        if (currentChannels.none { it == NOTIFICATION_CHANNEL_INCOMING_CALLS_ID }) {
            newChannels.add(
                NotificationChannelCompat.Builder(
                    NOTIFICATION_CHANNEL_INCOMING_CALLS_ID,
                    NotificationManager.IMPORTANCE_HIGH
                ).setName(NOTIFICATION_CHANNEL_INCOMING_CALLS_NAME).build()
            )
        }
        if (currentChannels.none { it == NOTIFICATION_CHANNEL_INPROGRESS_MISSED_CALLS_ID }) {
            newChannels.add(
                NotificationChannelCompat.Builder(
                    NOTIFICATION_CHANNEL_INPROGRESS_MISSED_CALLS_ID,
                    NotificationManager.IMPORTANCE_HIGH
                ).setName(NOTIFICATION_CHANNEL_INPROGRESS_MISSED_CALLS_NAME).build()
            )
        }

        if (newChannels.isNotEmpty()) {
            notificationManager.createNotificationChannelsCompat(newChannels)
        }
    }
}
