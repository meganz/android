package mega.privacy.android.app.presentation.transfers.notification

import mega.privacy.android.icon.pack.R as iconPackR
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.R
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.manager.model.TransfersTab
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.data.mapper.transfer.ChatUploadNotificationMapper
import mega.privacy.android.data.mapper.transfer.VideoCompressionProgress
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import javax.inject.Inject

/**
 * Default implementation of [ChatUploadNotificationMapper]
 */
class DefaultChatUploadNotificationMapper @Inject constructor(
    @ApplicationContext private val context: Context,
) : ChatUploadNotificationMapper {

    override fun invoke(
        activeTransferTotals: ActiveTransferTotals?,
        videoCompressionProgress: VideoCompressionProgress?,
        paused: Boolean,
    ): Notification {
        val intent = Intent(context, ManagerActivity::class.java)
        intent.action = Constants.ACTION_SHOW_TRANSFERS
        intent.putExtra(ManagerActivity.TRANSFERS_TAB, TransfersTab.PENDING_TAB)
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val content = context.getString(R.string.chat_upload_title_notification)

        val title = when {
            videoCompressionProgress != null -> {
                context.getString(
                    R.string.title_compress_video,
                    videoCompressionProgress.alreadyCompressed + 1,
                    videoCompressionProgress.totalToCompress
                )
            }

            (activeTransferTotals == null || activeTransferTotals.totalBytes == 0L) -> {
                context.getString(R.string.download_preparing_files)
            }

            else -> {
                val inProgress = activeTransferTotals.totalFinishedFileTransfers + 1
                val totalTransfers = activeTransferTotals.totalFileTransfers

                context.getString(
                    if (paused || activeTransferTotals.allPaused()) {
                        R.string.upload_service_notification_paused
                    } else {
                        R.string.upload_service_notification
                    },
                    inProgress,
                    totalTransfers
                )
            }
        }

        val builder = NotificationCompat.Builder(
            context,
            Constants.NOTIFICATION_CHANNEL_CHAT_UPLOAD_ID
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
        }
        return builder.build()
    }
}
