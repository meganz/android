package mega.privacy.android.app.presentation.transfers.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.transfers.TransfersActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Util
import mega.privacy.android.data.mapper.transfer.TransfersNotificationMapper
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.icon.pack.R as iconPackR
import javax.inject.Inject

/**
 * Default implementation of [TransfersNotificationMapper]
 */
class DefaultTransfersNotificationMapper @Inject constructor(
    @ApplicationContext private val context: Context,
) : TransfersNotificationMapper {

    override suspend fun invoke(
        activeTransferTotals: ActiveTransferTotals?,
        paused: Boolean,
    ): Notification {
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            0,
            TransfersActivity.getActiveTabIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val content = context.getString(R.string.download_touch_to_show)
        val title =
            if (activeTransferTotals == null || activeTransferTotals.totalBytes == 0L) {
                context.getString(R.string.download_preparing_files)
            } else {
                val inProgress = activeTransferTotals.totalFinishedFileTransfers + 1
                val totalTransfers = activeTransferTotals.totalFileTransfers
                val isDownload = activeTransferTotals.transfersType == TransferType.DOWNLOAD
                val areTransfersPaused = paused || activeTransferTotals.allPaused()
                val stringId = when {
                    isDownload && areTransfersPaused ->
                        R.string.download_service_notification_paused

                    !isDownload && areTransfersPaused ->
                        R.string.upload_service_notification_paused

                    isDownload -> R.string.download_service_notification
                    else -> R.string.upload_service_notification
                }

                context.getString(stringId, inProgress, totalTransfers)
            }
        val subText = activeTransferTotals?.let {
            Util.getProgressSize(
                context,
                activeTransferTotals.transferredBytes,
                activeTransferTotals.totalBytes
            )
        }

        val builder = NotificationCompat.Builder(
            context,
            Constants.NOTIFICATION_CHANNEL_DOWNLOAD_ID
        ).apply {
            setSmallIcon(iconPackR.drawable.ic_stat_notify)
            setOngoing(true)
            setContentTitle(title)
            setStyle(NotificationCompat.BigTextStyle().bigText(content))
            setContentText(content)
            setOnlyAlertOnce(true)
            setAutoCancel(false)
            setContentIntent(pendingIntent)
            activeTransferTotals?.transferProgress?.let { setProgress(100, it.intValue, false) }
            subText?.let { setSubText(subText) }
        }
        return builder.build()
    }
}
