package mega.privacy.android.data.worker

import android.app.Notification
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
import mega.privacy.android.data.mapper.transfer.TransfersActionGroupFinishNotificationBuilder
import mega.privacy.android.data.mapper.transfer.TransfersActionGroupProgressNotificationBuilder
import mega.privacy.android.data.mapper.transfer.TransfersFinishNotificationSummaryBuilder
import mega.privacy.android.data.mapper.transfer.TransfersNotificationMapper
import mega.privacy.android.data.mapper.transfer.TransfersProgressNotificationSummaryBuilder
import mega.privacy.android.data.qualifier.DisplayPathFromUriCache
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.TransferProgressResult
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.monitoring.CrashReporter
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.qualifier.LoginMutex
import mega.privacy.android.domain.usecase.transfers.MonitorActiveAndPendingTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.active.ClearActiveTransfersIfFinishedUseCase
import mega.privacy.android.domain.usecase.transfers.active.CorrectActiveTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.active.GetActiveTransferTotalsUseCase
import mega.privacy.android.domain.usecase.transfers.paused.AreTransfersPausedUseCase
import mega.privacy.android.domain.usecase.transfers.pending.StartAllPendingUploadsUseCase
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
    private val monitorActiveAndPendingTransfersUseCase: MonitorActiveAndPendingTransfersUseCase,
    areTransfersPausedUseCase: AreTransfersPausedUseCase,
    getActiveTransferTotalsUseCase: GetActiveTransferTotalsUseCase,
    overQuotaNotificationBuilder: OverQuotaNotificationBuilder,
    notificationManager: NotificationManagerCompat,
    areNotificationsEnabledUseCase: AreNotificationsEnabledUseCase,
    correctActiveTransfersUseCase: CorrectActiveTransfersUseCase,
    clearActiveTransfersIfFinishedUseCase: ClearActiveTransfersIfFinishedUseCase,
    private val transfersNotificationMapper: TransfersNotificationMapper,
    crashReporter: CrashReporter,
    foregroundSetter: ForegroundSetter? = null,
    notificationSamplePeriod: Long? = null,
    private val startAllPendingUploadsUseCase: StartAllPendingUploadsUseCase,
    @LoginMutex loginMutex: Mutex,
    private val transfersProgressNotificationSummaryBuilder: TransfersProgressNotificationSummaryBuilder,
    private val transfersActionGroupProgressNotificationBuilder: TransfersActionGroupProgressNotificationBuilder,
    private val transfersFinishNotificationSummaryBuilder: TransfersFinishNotificationSummaryBuilder,
    private val transfersActionGroupFinishNotificationBuilder: TransfersActionGroupFinishNotificationBuilder,
    @DisplayPathFromUriCache private val displayPathFromUriCache: HashMap<String, String>,
) : AbstractTransfersWorker(
    context = context,
    workerParams = workerParams,
    type = TransferType.GENERAL_UPLOAD,
    ioDispatcher = ioDispatcher,
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
    displayPathFromUriCache = displayPathFromUriCache,
) {
    override val finalNotificationId = NOTIFICATION_UPLOAD_FINAL
    override val updateNotificationId = UPLOAD_NOTIFICATION_ID

    override fun monitorProgress(): Flow<TransferProgressResult> =
        monitorActiveAndPendingTransfersUseCase(type)

    override suspend fun doWorkInternal(scope: CoroutineScope) {
        scope.launch {
            startAllPendingUploadsUseCase()
                .catch {
                    Timber.e(it, "Error on start uploading files")
                    crashReporter.report(it)
                }
                .collect { Timber.d("Start uploading $it files") }
        }
    }

    override suspend fun createProgressSummaryNotification(): Notification =
        transfersProgressNotificationSummaryBuilder(type)

    override suspend fun createActionGroupProgressNotification(
        actionGroup: ActiveTransferTotals.ActionGroup,
        paused: Boolean,
    ): Notification = transfersActionGroupProgressNotificationBuilder(actionGroup, type, paused)

    override suspend fun createFinishSummaryNotification(): Notification =
        transfersFinishNotificationSummaryBuilder(type)

    override suspend fun createActionGroupFinishNotification(actionGroup: ActiveTransferTotals.ActionGroup): Notification =
        transfersActionGroupFinishNotificationBuilder(actionGroup, type)

    override suspend fun createUpdateNotification(
        activeTransferTotals: ActiveTransferTotals,
        paused: Boolean,
    ) = transfersNotificationMapper(activeTransferTotals, paused)

    companion object {
        /**
         * Tag for enqueue the worker to work manager
         */
        const val SINGLE_UPLOAD_TAG = "MEGA_UPLOAD_TAG"
        internal const val UPLOAD_NOTIFICATION_ID = 1
        internal const val NOTIFICATION_UPLOAD_FINAL = 5
    }
}