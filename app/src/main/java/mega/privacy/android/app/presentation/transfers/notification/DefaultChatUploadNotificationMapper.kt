package mega.privacy.android.app.presentation.transfers.notification

import mega.privacy.android.icon.pack.R as iconPackR
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.R
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.manager.model.TransfersTab
import mega.privacy.android.app.presentation.transfers.EXTRA_TAB
import mega.privacy.android.app.presentation.transfers.TransfersActivity
import mega.privacy.android.app.presentation.transfers.view.IN_PROGRESS_TAB_INDEX
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.data.mapper.transfer.ChatUploadNotificationMapper
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.ChatCompressionProgress
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import javax.inject.Inject

/**
 * Default implementation of [ChatUploadNotificationMapper]
 */
class DefaultChatUploadNotificationMapper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
) : ChatUploadNotificationMapper {

    override suspend fun invoke(
        activeTransferTotals: ActiveTransferTotals?,
        chatCompressionProgress: ChatCompressionProgress?,
        paused: Boolean,
    ): Notification {
        val intent = if (getFeatureFlagValueUseCase(AppFeatures.TransfersSection)) {
            Intent(context, TransfersActivity::class.java).apply {
                putExtra(EXTRA_TAB, IN_PROGRESS_TAB_INDEX)
            }
        } else {
            Intent(context, ManagerActivity::class.java).apply {
                action = Constants.ACTION_SHOW_TRANSFERS
                putExtra(ManagerActivity.TRANSFERS_TAB, TransfersTab.PENDING_TAB)
            }
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val content = context.getString(R.string.chat_upload_title_notification)
        val progress: Progress?

        val title = when {
            chatCompressionProgress != null -> {
                progress = chatCompressionProgress.progress
                context.getString(
                    R.string.title_compress_video,
                    (chatCompressionProgress.alreadyCompressed + 1)
                        .coerceAtMost(chatCompressionProgress.totalToCompress),
                    chatCompressionProgress.totalToCompress
                )
            }

            (activeTransferTotals == null || activeTransferTotals.totalBytes == 0L) -> {
                progress = null
                context.getString(R.string.download_preparing_files)
            }

            else -> {
                progress = activeTransferTotals.transferProgress
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
            progress?.let { setProgress(100, it.intValue, false) }
        }
        return builder.build()
    }
}
