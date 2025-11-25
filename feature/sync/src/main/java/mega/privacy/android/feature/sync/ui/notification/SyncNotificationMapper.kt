package mega.privacy.android.feature.sync.ui.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import mega.privacy.android.domain.usecase.domainmigration.GetDomainNameUseCase
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationMessage
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationType
import mega.privacy.android.feature.sync.navigation.getSyncListRoute
import mega.privacy.android.feature.sync.ui.notification.SyncNotificationManager.Companion.CHANNEL_ID
import mega.privacy.android.feature.sync.ui.synclist.SyncChip
import mega.privacy.android.icon.pack.R
import javax.inject.Inject

/**
 * Mapper class to map a [SyncNotificationMessage] to a [Notification]
 */
class SyncNotificationMapper @Inject constructor(
    private val getDomainNameUseCase: GetDomainNameUseCase,
    private val syncPendingIntentProvider: SyncPendingIntentProvider,
) {

    operator fun invoke(
        context: Context,
        syncNotificationMessage: SyncNotificationMessage,
        singleActivity: Boolean,
    ): Notification {
        val pendingIntent = if (singleActivity) {
            syncPendingIntentProvider(context, syncNotificationMessage)
        } else {
            val androidSyncIntent = Intent(
                Intent.ACTION_VIEW, "https://${getDomainNameUseCase()}/${
                    getSyncListRoute(
                        selectedChip = when (syncNotificationMessage.syncNotificationType) {
                            SyncNotificationType.STALLED_ISSUE -> SyncChip.STALLED_ISSUES
                            else -> SyncChip.SYNC_FOLDERS
                        }
                    )
                }".toUri()
            )
            PendingIntent.getActivity(
                context,
                0,
                androidSyncIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle(context.getString(syncNotificationMessage.title))
            .setContentText(context.getString(syncNotificationMessage.text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
    }
}
