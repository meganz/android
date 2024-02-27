package mega.privacy.android.data.worker

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import mega.privacy.android.data.mapper.transfer.DownloadNotificationMapper
import mega.privacy.android.data.mapper.transfer.OverQuotaNotificationBuilder
import mega.privacy.android.data.mapper.transfer.TransfersFinishedNotificationMapper
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.qrcode.ScanMediaFileUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransferEventsUseCase
import mega.privacy.android.domain.usecase.transfers.active.AddOrUpdateActiveTransferUseCase
import mega.privacy.android.domain.usecase.transfers.active.ClearActiveTransfersIfFinishedUseCase
import mega.privacy.android.domain.usecase.transfers.active.CorrectActiveTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.active.GetActiveTransferTotalsUseCase
import mega.privacy.android.domain.usecase.transfers.active.MonitorOngoingActiveTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.paused.AreTransfersPausedUseCase
import mega.privacy.android.domain.usecase.transfers.sd.HandleSDCardEventUseCase
import timber.log.Timber

/**
 * Worker that will monitor current active downloads transfers while there are some.
 * This should be used once the downloads are actually started, it won't start any download.
 */
@HiltWorker
class DownloadsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    monitorTransferEventsUseCase: MonitorTransferEventsUseCase,
    addOrUpdateActiveTransferUseCase: AddOrUpdateActiveTransferUseCase,
    monitorOngoingActiveTransfersUseCase: MonitorOngoingActiveTransfersUseCase,
    areTransfersPausedUseCase: AreTransfersPausedUseCase,
    getActiveTransferTotalsUseCase: GetActiveTransferTotalsUseCase,
    overQuotaNotificationBuilder: OverQuotaNotificationBuilder,
    notificationManager: NotificationManagerCompat,
    areNotificationsEnabledUseCase: AreNotificationsEnabledUseCase,
    correctActiveTransfersUseCase: CorrectActiveTransfersUseCase,
    clearActiveTransfersIfFinishedUseCase: ClearActiveTransfersIfFinishedUseCase,
    private val downloadNotificationMapper: DownloadNotificationMapper,
    private val transfersFinishedNotificationMapper: TransfersFinishedNotificationMapper,
    private val handleSDCardEventUseCase: HandleSDCardEventUseCase,
    private val scanMediaFileUseCase: ScanMediaFileUseCase,
) : AbstractTransfersWorker(
    context,
    workerParams,
    TransferType.DOWNLOAD,
    ioDispatcher,
    monitorTransferEventsUseCase,
    addOrUpdateActiveTransferUseCase,
    monitorOngoingActiveTransfersUseCase,
    areTransfersPausedUseCase,
    getActiveTransferTotalsUseCase,
    overQuotaNotificationBuilder,
    notificationManager,
    areNotificationsEnabledUseCase,
    correctActiveTransfersUseCase,
    clearActiveTransfersIfFinishedUseCase,
) {

    override val finalNotificationId = DOWNLOAD_NOTIFICATION_ID
    override val updateNotificationId = NOTIFICATION_DOWNLOAD_FINAL

    override suspend fun createUpdateNotification(
        activeTransferTotals: ActiveTransferTotals,
        paused: Boolean,
    ) = downloadNotificationMapper(activeTransferTotals, paused)

    override suspend fun createFinishNotification(activeTransferTotals: ActiveTransferTotals) =
        transfersFinishedNotificationMapper(activeTransferTotals)

    override suspend fun onTransferEventReceived(event: TransferEvent) {
        handleSDCardEventUseCase(event)
        if (event is TransferEvent.TransferFinishEvent) {
            runCatching {
                scanMediaFileUseCase(arrayOf(event.transfer.localPath), arrayOf(""))
            }.onFailure {
                Timber.w(it, "Exception scanning file.")
            }
        }
    }

    companion object {
        /**
         * Tag for enqueue the worker to work manager
         */
        const val SINGLE_DOWNLOAD_TAG = "MEGA_DOWNLOAD_TAG"
        private const val DOWNLOAD_NOTIFICATION_ID = 2
        private const val NOTIFICATION_DOWNLOAD_FINAL = 4
    }
}

