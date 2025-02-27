package mega.privacy.android.app.presentation.transfers.notification

import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR
import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.filestorage.FileStorageActivity
import mega.privacy.android.app.presentation.manager.model.TransfersTab
import mega.privacy.android.app.presentation.transfers.EXTRA_TAB
import mega.privacy.android.app.presentation.transfers.TransfersActivity
import mega.privacy.android.app.presentation.transfers.view.COMPLETED_TAB_INDEX
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Util
import mega.privacy.android.data.mapper.transfer.TransfersActionGroupProgressNotificationBuilder
import mega.privacy.android.data.worker.AbstractTransfersWorker.Companion.PROGRESS_SUMMARY_GROUP
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.isOfflineDownload
import mega.privacy.android.domain.entity.transfer.isPreviewDownload
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.file.GetPathByDocumentContentUriUseCase
import mega.privacy.android.domain.usecase.file.IsContentUriUseCase
import mega.privacy.android.domain.usecase.transfers.paused.PauseTransfersQueueUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Default implementation of [TransfersActionGroupProgressNotificationBuilder]
 */
class DefaultTransfersActionGroupProgressNotificationBuilder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val isContentUriUseCase: IsContentUriUseCase,
    private val getPathByDocumentContentUriUseCase: GetPathByDocumentContentUriUseCase,
) : TransfersActionGroupProgressNotificationBuilder {
    private val resources get() = context.resources
    override suspend fun invoke(
        group: ActiveTransferTotals.Group,
        transferType: TransferType,
        paused: Boolean,
    ): Notification {
        val isDownload = transferType == TransferType.DOWNLOAD
        if (!isDownload) {
            throw NotImplementedError("Group notifications are not yet implemented for uploads")
        }
        val isPreviewDownload = group.isPreviewDownload()
        val isPreviewPaused = isPreviewDownload && paused
        val isOfflineDownload = group.isOfflineDownload()
        val notificationTitle = when {
            isPreviewPaused -> {
                resources.getString(
                    sharedR.string.transfers_notification_downloading_preview_paused,
                    group.singleFileName
                )
            }

            isPreviewDownload -> {
                resources.getString(
                    sharedR.string.transfers_notification_downloading_preview,
                    group.singleFileName
                )
            }

            group.totalBytes == 0L -> {
                context.getString(R.string.download_preparing_files)
            }

            else -> {
                val inProgress = group.finishedFiles + 1
                val totalTransfers = group.totalFiles
                val areTransfersPaused = paused || group.allPaused()

                val stringId = when {
                    areTransfersPaused -> R.string.download_service_notification_paused

                    else -> R.string.download_service_notification
                }

                context.getString(stringId, inProgress, totalTransfers)
            }
        }
        val subText = Util.getProgressSize(
            context,
            group.transferredBytes,
            group.totalBytes
        )
        val destination = runCatching {
            if (isContentUriUseCase(group.destination)) {
                getPathByDocumentContentUriUseCase(group.destination)
            } else {
                group.destination
            }
        }.getOrNull() ?: group.destination
        val destinationText = when {
            isOfflineDownload -> {
                context.getString(R.string.section_saved_for_offline_new)
            }

            isPreviewDownload -> {
                null
            }

            else -> {
                destination
            }
        }
        val contentText = destinationText?.let {
            resources.getString(
                sharedR.string.transfers_notification_location_content,
                it,
            )
        }
        val cancelTransferIntent = Intent(context, ManagerActivity::class.java).apply {
            action = Constants.ACTION_CANCEL_TRANSFER
            putExtra(Constants.INTENT_EXTRA_TAG, group.singleTransferTag)
        }
        val locateFileIntent = Intent(context, ManagerActivity::class.java).apply {
            action = Constants.ACTION_LOCATE_DOWNLOADED_FILE
            putExtra(Constants.INTENT_EXTRA_IS_OFFLINE_PATH, isOfflineDownload)
            putExtra(FileStorageActivity.EXTRA_PATH, destination)
            group.singleFileName?.let {
                putExtra(FileStorageActivity.EXTRA_FILE_NAME, it)
            }
        }
        val openTransfersSectionIntent =
            if (getFeatureFlagValueUseCase(AppFeatures.TransfersSection)) {
                Intent(context, TransfersActivity::class.java).apply {
                    putExtra(EXTRA_TAB, COMPLETED_TAB_INDEX)
                }
            } else {
                Intent(context, ManagerActivity::class.java).apply {
                    action = Constants.ACTION_SHOW_TRANSFERS
                    putExtra(ManagerActivity.TRANSFERS_TAB, TransfersTab.COMPLETED_TAB)
                }
            }
        val pendingIntent = if (!isPreviewDownload) {
            PendingIntent.getActivity(
                context,
                0,
                openTransfersSectionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            null
        }
        val actionPendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis()
                .toInt(), // Unique request code to make sure old intents are not reused
            if (isPreviewDownload) {
                cancelTransferIntent
            } else {
                locateFileIntent
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val actionText = when {
            isPreviewDownload -> {
                resources.getString(sharedR.string.general_dialog_cancel_button)
            }

            else -> {
                resources.getString(R.string.download_touch_to_show)
            }
        }

        return NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_DOWNLOAD_ID)
            .setSmallIcon(iconPackR.drawable.ic_stat_notify)
            .setOngoing(true)
            .setTicker(notificationTitle)
            .setContentTitle(notificationTitle)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setOnlyAlertOnce(true)
            .setAutoCancel(false)
            .setSubText(subText)
            .setProgress(100, group.progress.intValue, false)
            .setContentIntent(pendingIntent)
            .apply {
                contentText?.let {
                    setContentText(it)
                }
                addAction(
                    iconPackR.drawable.ic_stat_notify,
                    actionText,
                    actionPendingIntent
                )
                if (isPreviewPaused) {
                    val resumeTransfersPendingIntent = PendingIntent.getBroadcast(
                        context,
                        0,
                        Intent(context, ResumeTransfersReceiver::class.java),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    addAction(
                        iconPackR.drawable.ic_stat_notify,
                        resources.getString(R.string.option_resume_transfers),
                        resumeTransfersPendingIntent
                    )
                }
            }
            .setGroup(PROGRESS_SUMMARY_GROUP + transferType.name)
            .build()
    }

    @AndroidEntryPoint
    internal class ResumeTransfersReceiver : BroadcastReceiver() {

        @Inject
        lateinit var pauseTransfersQueueUseCase: PauseTransfersQueueUseCase

        @Inject
        @ApplicationScope
        lateinit var applicationScope: CoroutineScope

        override fun onReceive(context: Context?, intent: Intent?) {
            applicationScope.launch {
                runCatching {
                    pauseTransfersQueueUseCase(false)
                }.onFailure {
                    Timber.e(it, "Error resuming transfers")
                }.onSuccess {
                    Timber.d("Transfers resumed from notification")
                }
            }
        }
    }
}