package mega.privacy.android.feature.sync.ui.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationMessage
import mega.privacy.android.feature.sync.navigation.getSyncListRoute
import mega.privacy.android.feature.sync.ui.notification.SyncNotificationManager.Companion.CHANNEL_ID
import mega.privacy.android.icon.pack.R
import javax.inject.Inject

/**
 * Mapper class to map a [SyncNotificationMessage] to a [Notification]
 */
class SyncNotificationMapper @Inject constructor() {

    operator fun invoke(
        context: Context,
        syncNotificationMessage: SyncNotificationMessage,
    ): Notification {
        val androidSyncIntent = Intent(Intent.ACTION_VIEW, Uri.parse(getSyncListRoute()))
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            androidSyncIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle(syncNotificationMessage.title)
            .setContentText(syncNotificationMessage.text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
    }
}