package mega.privacy.android.app.presentation.transfers.notification

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
import mega.privacy.android.app.presentation.manager.model.TransfersTab
import mega.privacy.android.app.presentation.transfers.preview.LoadingPreviewActivity
import mega.privacy.android.app.presentation.transfers.preview.LoadingPreviewActivity.Companion.EXTRA_TRANSFER_TAG
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Util
import mega.privacy.android.data.mapper.transfer.TransfersActionGroupProgressNotificationBuilder
import mega.privacy.android.data.worker.AbstractTransfersWorker.Companion.PROGRESS_SUMMARY_GROUP
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.isOfflineDownload
import mega.privacy.android.domain.entity.transfer.isPreviewDownload
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.file.GetPathByDocumentContentUriUseCase
import mega.privacy.android.domain.usecase.file.IsContentUriUseCase
import mega.privacy.android.domain.usecase.transfers.paused.PauseTransfersQueueUseCase
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR
import timber.log.Timber
import javax.inject.Inject

/**
 * Default implementation of [TransfersActionGroupProgressNotificationBuilder]
 */
class DefaultTransfersActionGroupProgressNotificationBuilder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val isContentUriUseCase: IsContentUriUseCase,
    private val getPathByDocumentContentUriUseCase: GetPathByDocumentContentUriUseCase,
    private val openTransfersSectionIntentMapper: OpenTransfersSectionIntentMapper,
) : TransfersActionGroupProgressNotificationBuilder {
    private val resources get() = context.resources
    override suspend fun invoke(
        actionGroup: ActiveTransferTotals.ActionGroup,
        transferType: TransferType,
        paused: Boolean,
    ): Notification {
        if (transferType != TransferType.GENERAL_UPLOAD && transferType != TransferType.DOWNLOAD) {
            throw NotImplementedError("Group notifications are not yet implemented for this type: $transferType")
        }
        val isDownload = transferType == TransferType.DOWNLOAD
        val isPreviewDownload = isDownload && actionGroup.isPreviewDownload()
        val isPreviewPaused = isPreviewDownload && paused
        val isOfflineDownload = isDownload && actionGroup.isOfflineDownload()
        val notificationTitle = when {
            isPreviewPaused -> {
                resources.getString(
                    sharedR.string.transfers_notification_downloading_preview_paused,
                    actionGroup.singleFileName
                )
            }

            isPreviewDownload -> {
                resources.getString(
                    sharedR.string.transfers_notification_downloading_preview,
                    actionGroup.singleFileName
                )
            }

            actionGroup.totalBytes == 0L -> {
                context.getString(R.string.download_preparing_files)
            }

            else -> {
                val inProgress = actionGroup.finishedFiles + 1
                val totalTransfers = actionGroup.totalFiles
                val areTransfersPaused = paused || actionGroup.allPaused()

                val stringId = when {
                    areTransfersPaused && isDownload -> R.string.download_service_notification_paused
                    areTransfersPaused /*&& !isDownload*/ -> R.string.upload_service_notification_paused
                    isDownload -> R.string.download_service_notification
                    else /*!isDownload*/ -> R.string.upload_service_notification
                }

                context.getString(stringId, inProgress, totalTransfers)
            }
        }
        val subText = Util.getProgressSize(
            context,
            actionGroup.transferredBytes,
            actionGroup.totalBytes
        )
        val destination = runCatching {
            if (isDownload && isContentUriUseCase(actionGroup.destination)) {
                getPathByDocumentContentUriUseCase(actionGroup.destination)
            } else {
                actionGroup.destination
            }
        }.getOrNull() ?: actionGroup.destination
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
        val (contentPendingIntent, actionIntent) = if (!isPreviewDownload) {
            val openTransfersSectionIntent =
                openTransfersSectionIntentMapper(TransfersTab.PENDING_TAB)
            PendingIntent.getActivity(
                context,
                0,
                openTransfersSectionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            ) to openTransfersSectionIntent
        } else {
            null to Intent(context, LoadingPreviewActivity::class.java).apply {
                putExtra(EXTRA_TRANSFER_TAG, actionGroup.singleTransferTag)
            }
        }
        val actionPendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis()
                .toInt(), // Unique request code to make sure old intents are not reused
            actionIntent,
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
            .setProgress(100, actionGroup.progress.intValue, false)
            .setContentIntent(contentPendingIntent)
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