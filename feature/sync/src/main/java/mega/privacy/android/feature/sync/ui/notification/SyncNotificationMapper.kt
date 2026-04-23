package mega.privacy.android.feature.sync.ui.notification

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import mega.privacy.android.feature.sync.R as SyncR
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationMessage
import mega.privacy.android.feature.sync.ui.notification.SyncNotificationManager.Companion.CHANNEL_ID
import mega.privacy.android.feature.sync.ui.notification.SyncNotificationManager.Companion.SYNC_PROGRESS_CHANNEL_ID
import mega.privacy.android.icon.pack.R
import javax.inject.Inject

/**
 * Mapper class to map a [SyncNotificationMessage] to a [Notification]
 */
class SyncNotificationMapper @Inject constructor(
    private val syncPendingIntentProvider: SyncPendingIntentProvider,
) {

    operator fun invoke(
        context: Context,
        syncNotificationMessage: SyncNotificationMessage,
    ): Notification {
        val pendingIntent = syncPendingIntentProvider(context, syncNotificationMessage)

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle(context.getString(syncNotificationMessage.title))
            .setContentText(
                syncNotificationMessage.formattedText
                    ?: context.getString(syncNotificationMessage.text)
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
    }

    fun createForegroundNotification(context: Context): Notification {
        return NotificationCompat.Builder(context, SYNC_PROGRESS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle(context.getString(SyncR.string.sync))
            .setContentText(context.getString(SyncR.string.sync_list_sync_state_syncing))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
}
