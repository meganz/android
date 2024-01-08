package mega.privacy.android.app.presentation.transfers.notification

import mega.privacy.android.icon.pack.R as iconPackR
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
import mega.privacy.android.app.presentation.mapper.file.FileSizeStringMapper
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.data.mapper.transfer.TransfersFinishedNotificationMapper
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import javax.inject.Inject

/**
 * Default implementation of [TransfersFinishedNotificationMapper]
 */
class DefaultTransfersFinishedNotificationMapper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileSizeStringMapper: FileSizeStringMapper,
) : TransfersFinishedNotificationMapper {

    private val resources get() = context.resources
    override suspend fun invoke(activeTransferTotals: ActiveTransferTotals): Notification {
        val totalCompleted = activeTransferTotals.totalCompletedFileTransfers
        val totalFinished = activeTransferTotals.totalFinishedFileTransfers
        val errorCount = activeTransferTotals.totalFinishedWithErrorsFileTransfers
        val alreadyDownloadedCount = activeTransferTotals.totalAlreadyDownloadedFiles

        val notificationTitle = if (totalCompleted != totalFinished) {
            resources.getQuantityString(
                R.plurals.download_service_final_notification_with_details,
                totalFinished,
                totalCompleted,
                totalFinished,
            )
        } else {
            resources.getQuantityString(
                R.plurals.download_service_final_notification,
                totalCompleted,
                totalCompleted
            )

        }
        val contentText = when {
            errorCount > 0 && alreadyDownloadedCount > 0 ->
                "${alreadyDownloadedMsg(alreadyDownloadedCount)}, ${errorMsg(errorCount)}"

            errorCount > 0 -> errorMsg(errorCount)
            alreadyDownloadedCount > 0 -> alreadyDownloadedMsg(alreadyDownloadedCount)
            else -> okayMsg(activeTransferTotals.transferredBytes)
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
            .setContentTitle(notificationTitle).setContentText(contentText)
            .setOngoing(false)
            .build()
    }

    private fun errorMsg(errorCount: Int) = resources.getQuantityString(
        R.plurals.download_service_failed,
        errorCount,
        errorCount
    )

    private fun alreadyDownloadedMsg(alreadyDownloadedCount: Int) = resources.getQuantityString(
        R.plurals.already_downloaded_service,
        alreadyDownloadedCount,
        alreadyDownloadedCount,
    )

    private fun okayMsg(bytes: Long) = resources.getString(
        R.string.general_total_size,
        fileSizeStringMapper(bytes)
    )
}