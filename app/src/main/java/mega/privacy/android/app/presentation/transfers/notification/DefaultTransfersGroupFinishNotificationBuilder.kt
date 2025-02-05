package mega.privacy.android.app.presentation.transfers.notification

import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR
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
import mega.privacy.android.app.presentation.transfers.EXTRA_TAB
import mega.privacy.android.app.presentation.transfers.TransfersActivity
import mega.privacy.android.app.presentation.transfers.view.COMPLETED_TAB_INDEX
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.data.mapper.transfer.TransfersGroupFinishNotificationBuilder
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.offline.IsOfflinePathUseCase
import javax.inject.Inject

class DefaultTransfersGroupFinishNotificationBuilder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val isOfflinePathUseCase: IsOfflinePathUseCase,
) : TransfersGroupFinishNotificationBuilder {
    private val resources get() = context.resources
    override suspend fun invoke(
        group: ActiveTransferTotals.Group,
        transferType: TransferType,
    ): Notification {
        val totalCompleted = group.completedFiles
        val totalFinished = group.finishedFiles
        val errorCount = group.finishedFilesWithErrors
        val alreadyTransferredCount = group.alreadyTransferred
        val isDownload = transferType == TransferType.DOWNLOAD
        if (!isDownload) {
            throw NotImplementedError("Group notifications are not yet implemented for uploads")
        }
        val titleSuffix = when {
            errorCount > 0 && alreadyTransferredCount > 0 ->
                "${alreadyMsg(alreadyTransferredCount)}, ${errorMsg(errorCount)}"

            errorCount > 0 -> errorMsg(errorCount)
            alreadyTransferredCount > 0 -> alreadyMsg(alreadyTransferredCount)
            else -> null
        }
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
        } + (titleSuffix?.let { ". $it." } ?: "")

        val destination = if (isOfflinePathUseCase(group.destination)) {
            context.getString(R.string.section_saved_for_offline_new)
        } else {
            group.destination
        }
        val contentText = resources.getString(
            sharedR.string.transfers_notification_location_content,
            destination,
        )

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
        val actionPendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent, //to be changed in TRAN-745
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_DOWNLOAD_ID)
            .setSmallIcon(iconPackR.drawable.ic_stat_notify)
            .setColor(ContextCompat.getColor(context, R.color.red_600_red_300))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true).setTicker(notificationTitle)
            .setContentTitle(notificationTitle).setContentText(contentText)
            .setOngoing(false)
            .addAction(
                iconPackR.drawable.ic_stat_notify,
                resources.getString(sharedR.string.transfers_notification_location_action),
                actionPendingIntent
            )
            .setGroup(transferType.name)
            .build()
    }

    private fun errorMsg(errorCount: Int) = resources.getQuantityString(
        R.plurals.download_service_failed,
        errorCount,
        errorCount
    )

    private fun alreadyMsg(alreadyDownloadedCount: Int) =
        resources.getQuantityString(
            R.plurals.already_downloaded_service,
            alreadyDownloadedCount,
            alreadyDownloadedCount,
        )
}