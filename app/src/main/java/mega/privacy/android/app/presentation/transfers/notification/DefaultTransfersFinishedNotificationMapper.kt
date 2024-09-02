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
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.manager.model.TransfersTab
import mega.privacy.android.app.presentation.mapper.file.FileSizeStringMapper
import mega.privacy.android.app.presentation.transfers.EXTRA_TAB
import mega.privacy.android.app.presentation.transfers.TransfersActivity
import mega.privacy.android.app.presentation.transfers.view.COMPLETED_TAB_INDEX
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.data.mapper.transfer.TransfersFinishedNotificationMapper
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import javax.inject.Inject

/**
 * Default implementation of [TransfersFinishedNotificationMapper]
 */
class DefaultTransfersFinishedNotificationMapper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileSizeStringMapper: FileSizeStringMapper,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
) : TransfersFinishedNotificationMapper {

    private val resources get() = context.resources
    override suspend fun invoke(
        activeTransferTotals: ActiveTransferTotals,
    ): Notification {
        val totalCompleted = activeTransferTotals.totalCompletedFileTransfers
        val totalFinished = activeTransferTotals.totalFinishedFileTransfers
        val errorCount = activeTransferTotals.totalFinishedWithErrorsFileTransfers
        val alreadyTransferredCount = activeTransferTotals.totalAlreadyTransferredFiles
        val isDownload = activeTransferTotals.transfersType == TransferType.DOWNLOAD

        val notificationTitle = when {
            isDownload && totalCompleted != totalFinished -> {
                resources.getQuantityString(
                    R.plurals.download_service_final_notification_with_details,
                    totalFinished,
                    totalCompleted,
                    totalFinished,
                )
            }

            isDownload -> {
                resources.getQuantityString(
                    R.plurals.download_service_final_notification,
                    totalCompleted,
                    totalCompleted
                )
            }

            else -> {
                val title = resources.getQuantityString(
                    R.plurals.upload_service_final_notification,
                    totalCompleted,
                    totalCompleted
                )
                if (errorCount > 0) {
                    val error = resources.getQuantityString(
                        R.plurals.upload_service_failed,
                        errorCount, errorCount
                    )
                    "$title · $error"
                } else {
                    title
                }
            }
        }

        val contentText = when {
            errorCount > 0 && alreadyTransferredCount > 0 ->
                "${alreadyMsg(alreadyTransferredCount, isDownload)}, ${errorMsg(errorCount)}"

            isDownload && errorCount > 0 -> errorMsg(errorCount)
            alreadyTransferredCount > 0 -> alreadyMsg(alreadyTransferredCount, isDownload)
            else -> okayMsg(activeTransferTotals.transferredBytes)
        }

        val intent = if (getFeatureFlagValueUseCase(AppFeatures.TransfersSection)) {
            Intent(context, TransfersActivity::class.java).apply {
                putExtra(EXTRA_TAB, COMPLETED_TAB_INDEX)
            }
        } else {
            Intent(context, ManagerActivity::class.java).apply {
                action = Constants.ACTION_SHOW_TRANSFERS
                putExtra(ManagerActivity.TRANSFERS_TAB, TransfersTab.COMPLETED_TAB)
            }
        }
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

    private fun alreadyMsg(alreadyDownloadedCount: Int, isDownload: Boolean) =
        resources.getQuantityString(
            if (isDownload) R.plurals.already_downloaded_service else R.plurals.upload_service_notification_already_uploaded,
            alreadyDownloadedCount,
            alreadyDownloadedCount,
        )

    private fun okayMsg(bytes: Long) = resources.getString(
        R.string.general_total_size,
        fileSizeStringMapper(bytes)
    )
}