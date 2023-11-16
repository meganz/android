package mega.privacy.android.app.presentation.transfers.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.R
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.manager.model.TransfersTab
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Util
import mega.privacy.android.data.mapper.transfer.TransfersFinishedNotificationMapper
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.icon.pack.R as iconPackR
import javax.inject.Inject

/**
 * Default implementation of [TransfersFinishedNotificationMapper]
 */
class DefaultTransfersFinishedNotificationMapper @Inject constructor(
    @ApplicationContext private val context: Context,
) : TransfersFinishedNotificationMapper {

    private val resources get() = context.resources
    override suspend fun invoke(activeTransferTotals: ActiveTransferTotals): Notification {
        val notificationTitle: String
        val size: String
        val totalDownloads = activeTransferTotals.totalCompletedFileTransfers
        val errorCount = activeTransferTotals.totalFinishedNotCompletedFileTransfers
        if (errorCount > 0) {
            val totalNumber = activeTransferTotals.totalFinishedFileTransfers
            notificationTitle =
                resources.getQuantityString(
                    R.plurals.download_service_final_notification_with_details,
                    totalNumber,
                    totalDownloads,
                    totalNumber
                )
            size = resources.getQuantityString(
                R.plurals.download_service_failed,
                errorCount,
                errorCount
            )
        } else {
            notificationTitle =
                resources.getQuantityString(
                    R.plurals.download_service_final_notification,
                    totalDownloads,
                    totalDownloads
                )
            val totalBytes = Util.getSizeString(activeTransferTotals.transferredBytes, context)
            size = resources.getString(R.string.general_total_size, totalBytes)
        }
        val intent = Intent(context, ManagerActivity::class.java)
        intent.action = Constants.ACTION_SHOW_TRANSFERS
        intent.putExtra(ManagerActivity.TRANSFERS_TAB, TransfersTab.COMPLETED_TAB)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_DOWNLOAD_ID)
            .setSmallIcon(iconPackR.drawable.ic_stat_notify)
            .setColor(ContextCompat.getColor(context, R.color.red_600_red_300))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true).setTicker(notificationTitle)
            .setContentTitle(notificationTitle).setContentText(size)
            .setOngoing(false)
            .build()
    }
}