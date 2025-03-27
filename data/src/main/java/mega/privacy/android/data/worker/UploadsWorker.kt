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
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import mega.privacy.android.data.mapper.transfer.OverQuotaNotificationBuilder
import mega.privacy.android.data.mapper.transfer.TransfersFinishedNotificationMapper
import mega.privacy.android.data.mapper.transfer.TransfersNotificationMapper
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferProgressResult
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.monitoring.CrashReporter
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.qualifier.LoginMutex
import mega.privacy.android.domain.usecase.transfers.MonitorActiveAndPendingTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransferEventsUseCase
import mega.privacy.android.domain.usecase.transfers.active.ClearActiveTransfersIfFinishedUseCase
import mega.privacy.android.domain.usecase.transfers.active.CorrectActiveTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.active.GetActiveTransferTotalsUseCase
import mega.privacy.android.domain.usecase.transfers.paused.AreTransfersPausedUseCase
import mega.privacy.android.domain.usecase.transfers.pending.StartAllPendingUploadsUseCase
import mega.privacy.android.domain.usecase.transfers.uploads.SetNodeAttributesAfterUploadUseCase
import timber.log.Timber

/**
 * Worker that will monitor current active upload transfers while there are some.
 * This should be used once the uploads are actually started, it won't start any upload.
 */
@HiltWorker
class UploadsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    monitorTransferEventsUseCase: MonitorTransferEventsUseCase,
    private val monitorActiveAndPendingTransfersUseCase: MonitorActiveAndPendingTransfersUseCase,
    areTransfersPausedUseCase: AreTransfersPausedUseCase,
    getActiveTransferTotalsUseCase: GetActiveTransferTotalsUseCase,
    overQuotaNotificationBuilder: OverQuotaNotificationBuilder,
    notificationManager: NotificationManagerCompat,
    areNotificationsEnabledUseCase: AreNotificationsEnabledUseCase,
    correctActiveTransfersUseCase: CorrectActiveTransfersUseCase,
    clearActiveTransfersIfFinishedUseCase: ClearActiveTransfersIfFinishedUseCase,
    private val transfersNotificationMapper: TransfersNotificationMapper,
    private val transfersFinishedNotificationMapper: TransfersFinishedNotificationMapper,
    private val setNodeAttributesAfterUploadUseCase: SetNodeAttributesAfterUploadUseCase,
    crashReporter: CrashReporter,
    foregroundSetter: ForegroundSetter? = null,
    notificationSamplePeriod: Long? = null,
    private val startAllPendingUploadsUseCase: StartAllPendingUploadsUseCase,
    @LoginMutex private val loginMutex: Mutex,
) : AbstractTransfersWorker(
    context = context,
    workerParams = workerParams,
    type = TransferType.GENERAL_UPLOAD,
    ioDispatcher = ioDispatcher,
    monitorTransferEventsUseCase = monitorTransferEventsUseCase,
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
    loginMutex = loginMutex,
) {
    override val finalNotificationId = NOTIFICATION_UPLOAD_FINAL
    override val updateNotificationId = UPLOAD_NOTIFICATION_ID

    override fun monitorProgress(): Flow<TransferProgressResult> =
        monitorActiveAndPendingTransfersUseCase(type)

    override suspend fun doWorkInternal(scope: CoroutineScope) {
        scope.launch {
            super.doWorkInternal(this)
        }
        scope.launch {
            startAllPendingUploadsUseCase()
                .catch {
                    Timber.e(it, "Error on start uploading files")
                    crashReporter.report(it)
                }
                .collect { Timber.d("Start uploading $it files") }
        }
    }

    override suspend fun createUpdateNotification(
        activeTransferTotals: ActiveTransferTotals,
        paused: Boolean,
    ) = transfersNotificationMapper(activeTransferTotals, paused)

    override suspend fun createFinishNotification(activeTransferTotals: ActiveTransferTotals) =
        transfersFinishedNotificationMapper(activeTransferTotals)

    override suspend fun onTransferEventReceived(event: TransferEvent) {
        (event as? TransferEvent.TransferFinishEvent)?.let {
            if (it.error == null) {
                runCatching {
                    setNodeAttributesAfterUploadUseCase(
                        nodeHandle = it.transfer.nodeHandle,
                        uriPath = UriPath(it.transfer.localPath),
                        appData = it.transfer.appData
                    )
                }.onFailure { exception ->
                    Timber.e(exception, "Node attributes not correctly set")
                }
            }
        }
    }

    companion object {
        /**
         * Tag for enqueue the worker to work manager
         */
        const val SINGLE_UPLOAD_TAG = "MEGA_UPLOAD_TAG"
        private const val UPLOAD_NOTIFICATION_ID = 1
        private const val NOTIFICATION_UPLOAD_FINAL = 5
    }
}