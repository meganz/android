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
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.data.extensions.collectChunked
import mega.privacy.android.data.extensions.onFirst
import mega.privacy.android.data.extensions.skipUnstable
import mega.privacy.android.data.mapper.transfer.OverQuotaNotificationBuilder
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
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
import java.time.Instant
import java.time.Instant.MIN
import java.time.Instant.now
import kotlin.time.Duration.Companion.milliseconds

/**
 * Abstract CoroutineWorker to share common implementation of transfers workers
 * @param foregroundSetter to inject the set foreground method, used for testing
 * @param type Transfer type that this worker will manage
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
    private val notificationSamplePeriod: Long?,
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
    abstract suspend fun createUpdateNotification(
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

    open suspend fun onComplete() {}

    /**
     * @return a flow with updates of MonitorOngoingActiveTransfersResult to track the progress of the ongoing work. It will be sampled to avoid too much updates.
     */
    internal abstract fun monitorProgress(): Flow<MonitorProgressResult>

    internal fun consumeProgress() = monitorProgress()
        .skipUnstable(eventsChunkDuration) {
            // If there are no pending work the worker will finish, but as we are using `collectChunked` to monitor the events,
            // the pendingWork may not be updated, specifically after app restart. `skipUnstable` help to ensure we don't finish too soon.
            it.pendingWork
        }
        .onFirst({ it.pendingWork }) {
            // Signal to not kill the worker if the app is killed
            val foregroundInfo = getForegroundInfo(
                it.monitorOngoingActiveTransfersResult.activeTransferTotals,
                areTransfersPausedUseCase()
            )
            foregroundSetter?.setForeground(foregroundInfo) ?: run {
                crashReporter.log("${this@AbstractTransfersWorker::class.java.simpleName} start foreground")
                setForeground(foregroundInfo)
            }
        }
        .transformWhile {
            emit(it.monitorOngoingActiveTransfersResult)
            Timber.d("Progress emitted: $it.pendingWork ${it.monitorOngoingActiveTransfersResult.activeTransferTotals.hasOngoingTransfers()}")
            return@transformWhile it.pendingWork
        }
        .onEachSampled(
            notificationSamplePeriod ?: ON_TRANSFER_UPDATE_REFRESH_MILLIS
        ) { (transferTotals, paused, transferOverQuota, storageOverQuota) ->
            if (storageOverQuota || transferOverQuota) {
                showOverQuotaNotification(storageOverQuota = storageOverQuota)
            }
            //set progress percent as worker progress
            setProgress(workDataOf(PROGRESS to transferTotals.transferProgress.floatValue))
            //update the notification
            updateProgressNotification(createUpdateNotification(transferTotals, paused))
            Timber.d("${this@AbstractTransfersWorker::class.java.simpleName}${if (paused) "(paused) " else ""} Notification update (${transferTotals.transferProgress.intValue}):${transferTotals.hasOngoingTransfers()}")
        }
        .onCompletion {
            Timber.d("Worker monitor progress finished $it")
            onComplete()
        }
        .catch {
            Timber.e("${this@AbstractTransfersWorker::class.java.simpleName}error: $it")
            crashReporter.report(it)
        }

    /**
     * @return true if the work is completed, false otherwise
     * Work usually is completed when all the transfers are completed but can be overridden
     */
    protected open fun hasCompleted(activeTransferTotals: ActiveTransferTotals) =
        activeTransferTotals.hasCompleted()

    override suspend fun doWork() = withContext(ioDispatcher) {
        Timber.d("${this@AbstractTransfersWorker::class.java.simpleName} Started")
        crashReporter.log("${this@AbstractTransfersWorker::class.java.simpleName} Started")
        correctActiveTransfersUseCase(type) //to be sure we haven't missed any event before monitoring them
        val doWorkJob = this.launch(ioDispatcher) {
            doWorkInternal(this)
        }
        onStart()
        val lastMonitorOngoingActiveTransfersResult = consumeProgress().lastOrNull()

        stopWork(doWorkJob)
        lastMonitorOngoingActiveTransfersResult?.let { (lastActiveTransferTotals, _, transferOverQuota, storageOverQuota) ->
            if (hasCompleted(lastActiveTransferTotals)) {
                Timber.d("${this@AbstractTransfersWorker::class.java.simpleName} Finished Successful: $lastActiveTransferTotals")
                if (lastActiveTransferTotals.totalTransfers > 0) {
                    showFinishNotification(lastActiveTransferTotals)
                }
                return@withContext Result.success()
            } else {
                if (!storageOverQuota && !transferOverQuota) {
                    showFinishNotification(lastActiveTransferTotals)
                }
                Timber.d("${this@AbstractTransfersWorker::class.java.simpleName}finished Failure: $lastActiveTransferTotals")
                return@withContext Result.failure() // To retry in the future
            }
        }
        return@withContext Result.success() // If there are no ongoing transfers it means no more work needed
    }.also {
        crashReporter.log("${this@AbstractTransfersWorker::class.java.simpleName} Finished")
        Timber.d("${this@AbstractTransfersWorker::class.java.simpleName} Finished")
    }

    /**
     * Similar to onEach but it will run the action only if periodMillis time has passed since last time action was run
     * As opposite of the behaviour of Flow.sample, if an action is omitted because it won't be run later, it only runs when a new value is emitted
     * Action will always be run with first emitted value, action can be skipped with last emitted value.
     */
    private fun <T> Flow<T>.onEachSampled(
        periodMillis: Long,
        action: suspend (T) -> Unit,
    ): Flow<T> {
        var initial: Instant = MIN
        return this.transform {
            val now = now()
            if (periodMillis == 0L || now.isAfter(initial.plusMillis(periodMillis))) {
                initial = now
                action(it)
            }
            return@transform emit(it)
        }
    }

    override suspend fun getForegroundInfo() = getForegroundInfo(
        getActiveTransferTotalsUseCase(type),
        areTransfersPausedUseCase()
    )

    private suspend fun getForegroundInfo(
        activeTransferTotals: ActiveTransferTotals,
        paused: Boolean,
    ): ForegroundInfo =
        createForegroundInfo(createUpdateNotification(activeTransferTotals, paused))

    /**
     * Monitors transfer events and update the related active transfers
     */
    internal open suspend fun doWorkInternal(scope: CoroutineScope) {
        monitorTransferEventsUseCase()
            .filter { it.transfer.transferType == type }
            .collectChunked(
                chunkDuration = eventsChunkDuration,
                flushOnIdleDuration = 200.milliseconds
            ) { transferEvents ->
                scope.launch {
                    transferEvents.forEach {
                        onTransferEventReceived(it)
                    }
                }
                handleTransferEvents(transferEvents)
            }
    }

    private suspend fun handleTransferEvents(transferEvents: List<TransferEvent>) {
        withContext(NonCancellable) {
            //handling events can update Active transfers and ends the monitorOngoingActiveTransfers flow that triggers the cancelling of this job, so we need to launch it in a non cancellable context
            launch {
                handleTransferEventUseCase(events = transferEvents.toTypedArray())
            }
        }
    }

    private suspend fun stopWork(performWorkJob: Job) {
        Timber.d("${this@AbstractTransfersWorker::class.java.simpleName} Stop work")
        notificationManager.cancel(updateNotificationId)
        clearActiveTransfersIfFinishedUseCase()
        performWorkJob.cancel()
    }

    /**
     * Updates the notification that shows the transfer progress
     */
    @SuppressLint("MissingPermission")
    protected suspend fun updateProgressNotification(notification: Notification) {
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
    private suspend fun showFinalNotification(
        notification: Notification?,
        notificationId: Int?,
    ) {
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

        /**
         * Milliseconds to sample the transfer progress updates
         */
        const val ON_TRANSFER_UPDATE_REFRESH_MILLIS = 2000L

        /**
         * To improve performance, and avoid too much database transactions, transfer events are chunked this duration
         */
        private val eventsChunkDuration = ON_TRANSFER_UPDATE_REFRESH_MILLIS.milliseconds
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
