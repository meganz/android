package mega.privacy.android.data.worker

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import mega.privacy.android.data.mapper.transfer.OverQuotaNotificationBuilder
import mega.privacy.android.data.mapper.transfer.TransfersFinishedNotificationMapper
import mega.privacy.android.data.mapper.transfer.TransfersNotificationMapper
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.monitoring.CrashReporter
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.qrcode.ScanMediaFileUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransferEventsUseCase
import mega.privacy.android.domain.usecase.transfers.active.ClearActiveTransfersIfFinishedUseCase
import mega.privacy.android.domain.usecase.transfers.active.CorrectActiveTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.active.GetActiveTransferTotalsUseCase
import mega.privacy.android.domain.usecase.transfers.active.HandleTransferEventUseCase
import mega.privacy.android.domain.usecase.transfers.active.MonitorOngoingActiveTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.paused.AreTransfersPausedUseCase
import mega.privacy.android.domain.usecase.transfers.pending.GetPendingTransfersByTypeUseCase
import mega.privacy.android.domain.usecase.transfers.pending.StartAllPendingDownloadsUseCase
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
    handleTransferEventUseCase: HandleTransferEventUseCase,
    areTransfersPausedUseCase: AreTransfersPausedUseCase,
    getActiveTransferTotalsUseCase: GetActiveTransferTotalsUseCase,
    overQuotaNotificationBuilder: OverQuotaNotificationBuilder,
    notificationManager: NotificationManagerCompat,
    areNotificationsEnabledUseCase: AreNotificationsEnabledUseCase,
    correctActiveTransfersUseCase: CorrectActiveTransfersUseCase,
    clearActiveTransfersIfFinishedUseCase: ClearActiveTransfersIfFinishedUseCase,
    private val transfersNotificationMapper: TransfersNotificationMapper,
    private val transfersFinishedNotificationMapper: TransfersFinishedNotificationMapper,
    private val scanMediaFileUseCase: ScanMediaFileUseCase,
    private val crashReporter: CrashReporter,
    foregroundSetter: ForegroundSetter? = null,
    notificationSamplePeriod: Long? = null,
    private val monitorOngoingActiveTransfersUseCase: MonitorOngoingActiveTransfersUseCase,
    private val getPendingTransfersByTypeUseCase: GetPendingTransfersByTypeUseCase,
    private val startAllPendingDownloadsUseCase: StartAllPendingDownloadsUseCase,
) : AbstractTransfersWorker(
    context = context,
    workerParams = workerParams,
    type = TransferType.DOWNLOAD,
    ioDispatcher = ioDispatcher,
    monitorTransferEventsUseCase = monitorTransferEventsUseCase,
    handleTransferEventUseCase = handleTransferEventUseCase,
    areTransfersPausedUseCase = areTransfersPausedUseCase,
    getActiveTransferTotalsUseCase = getActiveTransferTotalsUseCase,
    overQuotaNotificationBuilder = overQuotaNotificationBuilder,
    notificationManager = notificationManager,
    areNotificationsEnabledUseCase = areNotificationsEnabledUseCase,
    correctActiveTransfersUseCase = correctActiveTransfersUseCase,
    clearActiveTransfersIfFinishedUseCase = clearActiveTransfersIfFinishedUseCase,
    crashReporter = crashReporter,
    foregroundSetter = foregroundSetter,
    notificationSamplePeriod = notificationSamplePeriod,
) {

    override val finalNotificationId = DOWNLOAD_FINAL_NOTIFICATION_ID
    override val updateNotificationId = DOWNLOAD_UPDATE_NOTIFICATION_ID

    override fun monitorProgress(): Flow<MonitorProgressResult> =
        combine(
            monitorOngoingActiveTransfersUseCase(type),
            getPendingTransfersByTypeUseCase(type),
        ) { ongoingActiveTransfersResult, pendingTransfersNotSend ->
            //keep monitoring if and only if there are pending transfers or transfers in progress
            val pendingWork =
                pendingTransfersNotSend.isNotEmpty() || ongoingActiveTransfersResult.hasPendingWork(type)
            MonitorProgressResult(ongoingActiveTransfersResult, pendingWork)
        }


    override suspend fun doWorkInternal(scope: CoroutineScope) {
        scope.launch {
            super.doWorkInternal(this)
        }
        scope.launch {
            startAllPendingDownloadsUseCase()
                .catch {
                    Timber.e("Error on start downloading nodes", it)
                    crashReporter.report(it)
                }
                .collect { Timber.d("Start downloading $it nodes") }
        }
    }

    override suspend fun createUpdateNotification(
        activeTransferTotals: ActiveTransferTotals,
        paused: Boolean,
    ) = transfersNotificationMapper(activeTransferTotals, paused)

    override suspend fun createFinishNotification(activeTransferTotals: ActiveTransferTotals) =
        transfersFinishedNotificationMapper(activeTransferTotals)

    override suspend fun onTransferEventReceived(event: TransferEvent) {
        if (event is TransferEvent.TransferFinishEvent && event.transfer.transferredBytes == event.transfer.totalBytes && event.error == null) {
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
        private const val DOWNLOAD_FINAL_NOTIFICATION_ID = 2
        private const val DOWNLOAD_UPDATE_NOTIFICATION_ID = 4
    }
}

