package mega.privacy.android.app.presentation.transfers.notification

import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.MimeTypeList.Companion.typeForName
import mega.privacy.android.app.R
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.main.FileStorageActivity
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.manager.model.TransfersTab
import mega.privacy.android.app.presentation.mapper.file.FileSizeStringMapper
import mega.privacy.android.app.presentation.transfers.EXTRA_TAB
import mega.privacy.android.app.presentation.transfers.TransfersActivity
import mega.privacy.android.app.presentation.transfers.view.COMPLETED_TAB_INDEX
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.MegaApiUtils
import mega.privacy.android.data.mapper.transfer.TransfersActionGroupFinishNotificationBuilder
import mega.privacy.android.data.worker.AbstractTransfersWorker.Companion.FINAL_SUMMARY_GROUP
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.isOfflineDownload
import mega.privacy.android.domain.entity.transfer.isPreviewDownload
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.file.GetPathByDocumentContentUriUseCase
import mega.privacy.android.domain.usecase.file.IsContentUriUseCase
import java.io.File
import javax.inject.Inject

/**
 * Default implementation of [TransfersActionGroupFinishNotificationBuilder]
 */
class DefaultTransfersActionGroupFinishNotificationBuilder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val isContentUriUseCase: IsContentUriUseCase,
    private val getPathByDocumentContentUriUseCase: GetPathByDocumentContentUriUseCase,
    private val fileSizeStringMapper: FileSizeStringMapper,
) : TransfersActionGroupFinishNotificationBuilder {
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
        val isPreviewDownload = group.isPreviewDownload()
        val isOfflineDownload = group.isOfflineDownload()
        val titleSuffix = when {
            errorCount > 0 && alreadyTransferredCount > 0 ->
                "${alreadyMsg(alreadyTransferredCount)}, ${errorMsg(errorCount)}"

            errorCount > 0 -> errorMsg(errorCount)
            alreadyTransferredCount > 0 -> alreadyMsg(alreadyTransferredCount)
            else -> null
        }
        val notificationTitle = when {
            totalCompleted != totalFinished -> {
                resources.getQuantityString(
                    R.plurals.download_service_final_notification_with_details,
                    totalFinished,
                    totalCompleted,
                    totalFinished,
                )
            }

            totalCompleted > 1 || group.singleFileName == null -> {
                resources.getQuantityString(
                    R.plurals.download_service_final_notification,
                    totalCompleted,
                    totalCompleted
                )
            }

            isPreviewDownload -> {
                resources.getString(
                    sharedR.string.transfers_notification_title_preview_download,
                    group.singleFileName
                )
            }

            else -> {
                resources.getString(
                    sharedR.string.transfers_notification_title_single_download,
                    group.singleFileName
                )
            }
        } + (titleSuffix?.let { ". $it." } ?: "")

        val destination = runCatching {
            if (isContentUriUseCase(group.destination)) {
                getPathByDocumentContentUriUseCase(group.destination)
            } else {
                group.destination
            }
        }.getOrNull() ?: group.destination
        val destinationText = when {
            isOfflineDownload -> context.getString(R.string.section_saved_for_offline_new)
            isPreviewDownload -> null
            else -> destination
        }
        val contentText = destinationText?.let {
            resources.getString(
                sharedR.string.transfers_notification_location_content,
                it,
            )
        }?.let {
            if (titleSuffix == null) {
                it + "\n" + resources.getString(
                    R.string.general_total_size,
                    fileSizeStringMapper(group.totalBytes)
                )
            } else {
                it
            }
        }

        val previewFile = File(group.destination + group.singleFileName)
        var previewIntent: Intent? = Intent(Intent.ACTION_VIEW).apply {
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        previewIntent =
            if (previewFile.exists() && MegaApiUtils.isIntentAvailable(context, previewIntent)) {
                FileProvider.getUriForFile(
                    context,
                    Constants.AUTHORITY_STRING_FILE_PROVIDER,
                    previewFile
                )?.let { uri ->
                    previewIntent?.let {
                        it.setDataAndType(uri, typeForName(previewFile.name).type)
                        val chooserTitle = resources.getString(
                            sharedR.string.open_with_os_dialog_title,
                            group.singleFileName
                        )
                        Intent.createChooser(it, chooserTitle)
                    }
                }
            } else {
                null
            } ?: run {
                val warningMessage = resources.getString(R.string.intent_not_available)
                Intent(context, ManagerActivity::class.java).apply {
                    action = Constants.ACTION_SHOW_WARNING
                    putExtra(Constants.INTENT_EXTRA_WARNING_MESSAGE, warningMessage)
                }
            }

        val intent = when {
            isPreviewDownload -> {
                previewIntent
            }

            getFeatureFlagValueUseCase(AppFeatures.TransfersSection) -> {
                Intent(context, TransfersActivity::class.java).apply {
                    putExtra(EXTRA_TAB, COMPLETED_TAB_INDEX)
                }
            }

            else -> {
                Intent(context, ManagerActivity::class.java).apply {
                    action = Constants.ACTION_SHOW_TRANSFERS
                    putExtra(ManagerActivity.TRANSFERS_TAB, TransfersTab.COMPLETED_TAB)
                }
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val actionIntent = if (isPreviewDownload) {
            previewIntent
        } else {
            Intent(context, ManagerActivity::class.java).apply {
                action = Constants.ACTION_LOCATE_DOWNLOADED_FILE
                putExtra(Constants.INTENT_EXTRA_IS_OFFLINE_PATH, isOfflineDownload)
                putExtra(FileStorageActivity.EXTRA_PATH, destination)
                group.singleFileName?.let {
                    putExtra(FileStorageActivity.EXTRA_FILE_NAME, it)
                }
            }
        }

        val actionPendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis()
                .toInt(), // Unique request code to make sure old intents are not reused
            actionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val actionText = if (isPreviewDownload) {
            resources.getString(sharedR.string.transfers_notification_preview_action)
        } else {
            resources.getString(sharedR.string.transfers_notification_location_action)
        }

        return NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_DOWNLOAD_ID)
            .setSmallIcon(iconPackR.drawable.ic_stat_notify)
            .setColor(ContextCompat.getColor(context, R.color.red_600_red_300))
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setAutoCancel(true)
            .setTicker(notificationTitle)
            .setContentTitle(notificationTitle)
            .setOngoing(false)
            .apply {
                contentText?.let {
                    setContentText(it)
                }
                if (group.completedFiles > 0 || group.alreadyTransferred > 0) {
                    addAction(
                        iconPackR.drawable.ic_stat_notify,
                        actionText,
                        actionPendingIntent
                    )
                }
            }
            .setGroup(FINAL_SUMMARY_GROUP + transferType.name)
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