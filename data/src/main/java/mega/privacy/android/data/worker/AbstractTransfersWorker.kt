package mega.privacy.android.data.worker

import android.annotation.SuppressLint
import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.data.mapper.transfer.OverQuotaNotificationBuilder
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.MonitorOngoingActiveTransfersResult
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.monitoring.CrashReporter
import mega.privacy.android.domain.usecase.transfers.MonitorTransferEventsUseCase
import mega.privacy.android.domain.usecase.transfers.active.ClearActiveTransfersIfFinishedUseCase
import mega.privacy.android.domain.usecase.transfers.active.CorrectActiveTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.active.GetActiveTransferTotalsUseCase
import mega.privacy.android.domain.usecase.transfers.active.HandleTransferEventUseCase
import mega.privacy.android.domain.usecase.transfers.paused.AreTransfersPausedUseCase
import timber.log.Timber

/**
 * Abstract CoroutineWorker to share common implementation of transfers workers
 * @param foregroundSetter to inject the set foreground method, used for testing
 */
abstract class AbstractTransfersWorker(
    context: Context,
    workerParams: WorkerParameters,
    protected val type: TransferType,
    private val ioDispatcher: CoroutineDispatcher,
    private val monitorTransferEventsUseCase: MonitorTransferEventsUseCase,
    private val handleTransferEventUseCase: HandleTransferEventUseCase,
    private val areTransfersPausedUseCase: AreTransfersPausedUseCase,
    private val getActiveTransferTotalsUseCase: GetActiveTransferTotalsUseCase,
    private val overQuotaNotificationBuilder: OverQuotaNotificationBuilder,
    private val notificationManager: NotificationManagerCompat,
    private val areNotificationsEnabledUseCase: AreNotificationsEnabledUseCase,
    private val correctActiveTransfersUseCase: CorrectActiveTransfersUseCase,
    private val clearActiveTransfersIfFinishedUseCase: ClearActiveTransfersIfFinishedUseCase,
    private val crashReporter: CrashReporter,
    private val foregroundSetter: ForegroundSetter?,
) : CoroutineWorker(context, workerParams) {

    /**
     * Update notification id
     */
    abstract val updateNotificationId: Int

    /**
     * Final notification Id, null if there's no final notification
     */
    open val finalNotificationId: Int? = null

    /**
     * Create the update Notification to show worker progress
     */
    abstract fun createUpdateNotification(
        activeTransferTotals: ActiveTransferTotals,
        paused: Boolean,
    ): Notification

    /**
     * Create the final notification to summarize the work done, null if there's no final notification
     */
    open suspend fun createFinishNotification(activeTransferTotals: ActiveTransferTotals): Notification? =
        null

    /**
     * Event to do extra work on starting the worker, just after correctActiveTransfersUseCase
     */
    open suspend fun onStart() {}

    /**
     * Event to do extra work on each transfer event
     */
    open suspend fun onTransferEventReceived(event: TransferEvent) {}

    /**
     * @return a flow with updates of MonitorOngoingActiveTransfersResult to track the progress of the ongoing work. It will be sampled to avoid too much updates.
     */
    abstract fun monitorOngoingActiveTransfers(): Flow<MonitorOngoingActiveTransfersResult>

    /**
     * @return true if the work is completed, false otherwise
     * Work usually is completed when all the transfers are completed but can be overridden
     */
    protected open fun hasCompleted(activeTransferTotals: ActiveTransferTotals) =
        activeTransferTotals.hasCompleted()

    @OptIn(FlowPreview::class)
    override suspend fun doWork() = withContext(ioDispatcher) {
        Timber.d("${this@AbstractTransfersWorker::class.java.simpleName} Started")
        crashReporter.log("${this@AbstractTransfersWorker::class.java.simpleName} Started")
        val monitorJob = monitorTransferEvents(this)
        correctActiveTransfersUseCase(type) //to be sure we haven't missed any event before monitoring them
        onStart()
        val lastMonitorOngoingActiveTransfersResult = monitorOngoingActiveTransfers()
            .catch { Timber.e("${this@AbstractTransfersWorker::class.java.simpleName}error: $it") }
            .onFirst {
                if (!hasCompleted(it.activeTransferTotals)) {
                    // Signal to not kill the worker if the app is killed
                    val foregroundInfo =
                        getForegroundInfo(it.activeTransferTotals, areTransfersPausedUseCase())
                    foregroundSetter?.setForeground(foregroundInfo) ?: run {
                        crashReporter.log("${this@AbstractTransfersWorker::class.java.simpleName} start foreground")
                        setForeground(foregroundInfo)
                    }
                }
            }
            .sample(ON_TRANSFER_UPDATE_REFRESH_MILLIS)
            .onEach { (transferTotals, paused, _, _) ->
                //set progress percent as worker progress
                setProgress(workDataOf(PROGRESS to transferTotals.transferProgress.floatValue))
                //update the notification
                notify(createUpdateNotification(transferTotals, paused))
                Timber.d("${this@AbstractTransfersWorker::class.java.simpleName}${if (paused) "(paused) " else ""} Notification update (${transferTotals.transferProgress.intValue}):${transferTotals.hasOngoingTransfers()}")
            }.lastOrNull()

        stopService(monitorJob)
        lastMonitorOngoingActiveTransfersResult?.let { (lastActiveTransferTotals, _, transferOverQuota, storageOverQuota) ->
            if (hasCompleted(lastActiveTransferTotals)) {
                Timber.d("${this@AbstractTransfersWorker::class.java.simpleName} Finished Successful: $lastActiveTransferTotals")
                if (lastActiveTransferTotals.totalTransfers > 0) {
                    showFinishNotification(lastActiveTransferTotals)
                }
                return@withContext Result.success()
            } else {
                if (storageOverQuota || transferOverQuota) {
                    showOverQuotaNotification(storageOverQuota = storageOverQuota)
                } else {
                    showFinishNotification(lastActiveTransferTotals)
                }
                Timber.d("${this@AbstractTransfersWorker::class.java.simpleName}finished Failure: $lastActiveTransferTotals")
                return@withContext Result.failure() // To retry in the future
            }
        }
        return@withContext Result.success() // If there are no ongoing transfers it means no more work needed
    }.also {
        crashReporter.log("${this@AbstractTransfersWorker::class.java.simpleName} Finished")
    }

    private fun <T> Flow<T>.onFirst(action: suspend (T) -> Unit): Flow<T> =
        this.withIndex().transform { (index, value) ->
            if (index == 0) action(value)
            return@transform emit(value)
        }

    override suspend fun getForegroundInfo() = getForegroundInfo(
        getActiveTransferTotalsUseCase(type),
        areTransfersPausedUseCase()
    )

    private fun getForegroundInfo(
        activeTransferTotals: ActiveTransferTotals,
        paused: Boolean,
    ): ForegroundInfo {
        return createForegroundInfo(createUpdateNotification(activeTransferTotals, paused))
    }

    /**
     * Monitors transfer events and update the related active transfers
     */
    private fun monitorTransferEvents(scope: CoroutineScope) =
        scope.launch(ioDispatcher) {
            monitorTransferEventsUseCase()
                .filter { it.transfer.transferType == type }
                .collect { transferEvent ->
                    onTransferEventReceived(transferEvent)
                    handleTransferEventUseCase(transferEvent)
                }
        }

    private suspend fun stopService(monitorJob: Job) {
        notificationManager.cancel(updateNotificationId)
        clearActiveTransfersIfFinishedUseCase(type)
        monitorJob.cancel()
    }

    @SuppressLint("MissingPermission")
    protected suspend fun notify(notification: Notification) {
        if (areNotificationsEnabledUseCase()) {
            notificationManager.notify(
                updateNotificationId,
                notification,
            )
        }
    }

    private suspend fun showFinishNotification(activeTransferTotals: ActiveTransferTotals) =
        showFinalNotification(
            createFinishNotification(activeTransferTotals),
            finalNotificationId,
        )

    private suspend fun showOverQuotaNotification(storageOverQuota: Boolean) =
        showFinalNotification(
            overQuotaNotificationBuilder(storageOverQuota = storageOverQuota),
            NOTIFICATION_STORAGE_OVERQUOTA
        )

    @SuppressLint("MissingPermission")
    private suspend fun showFinalNotification(notification: Notification?, notificationId: Int?) {
        if (notification != null && notificationId != null && areNotificationsEnabledUseCase()) {
            notificationManager.notify(
                notificationId,
                notification,
            )
        }
    }

    /**
     * Create a [ForegroundInfo] based on [Notification]
     */
    @SuppressLint("SpecifyForegroundServiceType")
    private fun createForegroundInfo(notification: Notification) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                updateNotificationId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(
                updateNotificationId,
                notification
            )
        }


    companion object {
        /**
         * Tag to get the progress as float in [0,1] range of this worker
         */
        const val PROGRESS = "Progress"
        private const val NOTIFICATION_STORAGE_OVERQUOTA = 14
        const val ON_TRANSFER_UPDATE_REFRESH_MILLIS = 2000L
    }
}

/**
 * Interface to inject the set foreground method, used for testing
 */
interface ForegroundSetter {
    /**
     * Set foreground
     */
    suspend fun setForeground(foregroundInfo: ForegroundInfo)
}