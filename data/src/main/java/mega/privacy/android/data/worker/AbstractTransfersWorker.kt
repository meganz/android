package mega.privacy.android.data.worker

import android.annotation.SuppressLint
import android.app.ForegroundServiceStartNotAllowedException
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import mega.privacy.android.data.mapper.transfer.OverQuotaNotificationBuilder
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.MonitorOngoingActiveTransfersResult
import mega.privacy.android.domain.entity.transfer.TransferProgressResult
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.extension.onEachSampled
import mega.privacy.android.domain.extension.onFirst
import mega.privacy.android.domain.extension.skipUnstable
import mega.privacy.android.domain.monitoring.CrashReporter
import mega.privacy.android.domain.qualifier.LoginMutex
import mega.privacy.android.domain.usecase.transfers.active.ClearActiveTransfersIfFinishedUseCase
import mega.privacy.android.domain.usecase.transfers.active.CorrectActiveTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.active.GetActiveTransferTotalsUseCase
import mega.privacy.android.domain.usecase.transfers.paused.AreTransfersPausedUseCase
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds

/**
 * Abstract CoroutineWorker to share common implementation of transfers workers
 * @param foregroundSetter to inject the set foreground method, used for testing
 * @param type Transfer type that this worker will manage
 * @param crashReporter CrashReporter to log information and errors
 */
abstract class AbstractTransfersWorker(
    context: Context,
    workerParams: WorkerParameters,
    protected val type: TransferType,
    private val ioDispatcher: CoroutineDispatcher,
    private val areTransfersPausedUseCase: AreTransfersPausedUseCase,
    private val getActiveTransferTotalsUseCase: GetActiveTransferTotalsUseCase,
    private val overQuotaNotificationBuilder: OverQuotaNotificationBuilder,
    private val notificationManager: NotificationManagerCompat,
    private val areNotificationsEnabledUseCase: AreNotificationsEnabledUseCase,
    private val correctActiveTransfersUseCase: CorrectActiveTransfersUseCase,
    private val clearActiveTransfersIfFinishedUseCase: ClearActiveTransfersIfFinishedUseCase,
    protected val crashReporter: CrashReporter,
    private val foregroundSetter: ForegroundSetter?,
    private val notificationSamplePeriod: Long?,
    @LoginMutex private val loginMutex: Mutex,
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
     * Event to do extra work on complete
     */
    open suspend fun onComplete() {}

    /**
     * @return a flow with updates of MonitorOngoingActiveTransfersResult to track the progress of the ongoing work. It will be sampled to avoid too much updates.
     */
    internal abstract fun monitorProgress(): Flow<TransferProgressResult>

    internal abstract suspend fun doWorkInternal(scope: CoroutineScope)

    internal fun consumeProgress() = monitorProgress()
        .skipUnstable(eventsChunkDuration) {
            // If there are no pending work the worker will finish, but as we are using `collectChunked` to monitor the events,
            // the pendingWork may not be updated, specifically after app restart. `skipUnstable` help to ensure we don't finish too soon.
            it.pendingWork
        }
        .onFirst({ it.ongoingTransfers }) {
            // Signal to not kill the worker if the app is killed when there are transfers in progress
            val foregroundInfo = getForegroundInfo(
                it.monitorOngoingActiveTransfersResult.activeTransferTotals,
                areTransfersPausedUseCase()
            )
            foregroundSetter?.setForeground(foregroundInfo) ?: run {
                val simpleName = this@AbstractTransfersWorker::class.java.simpleName
                Timber.d("$simpleName start foreground")
                crashReporter.log("$simpleName start foreground")
                runCatching {
                    setForeground(foregroundInfo)
                }.onFailure { e ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && e is ForegroundServiceStartNotAllowedException) {
                        // this is expected: https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work#backwards-compat
                        Timber.d(e, "$simpleName foreground service not allowed")
                    } else {
                        crashReporter.report(e)
                        Timber.e(e, "$simpleName failed to start foreground service")
                    }
                }
            }
        }
        .transformWhile {
            emit(it.monitorOngoingActiveTransfersResult)
            Timber.d("Progress emitted: $it.pendingWork ${it.monitorOngoingActiveTransfersResult.activeTransferTotals.hasOngoingTransfers()}")
            return@transformWhile it.pendingWork
        }
        .onEachSampled(
            (notificationSamplePeriod ?: ON_TRANSFER_UPDATE_REFRESH_MILLIS).milliseconds,
            shouldEmitImmediately = { previous, new ->
                hasAnyPausedChange(previous, new)
                        || new.activeTransferTotals.hasCompleted()
                        || new.activeTransferTotals.actionGroups.any { it.finished() }
            }
        ) { (transferTotals, paused, transferOverQuota, storageOverQuota) ->
            if (storageOverQuota || transferOverQuota) {
                showOverQuotaNotification(storageOverQuota = storageOverQuota)
            }
            if (showGroupedNotifications()) {
                checkFinishedGroups(transferTotals)
                checkProgressGroups(transferTotals, paused)
            }
            //set progress percent as worker progress
            setProgress(workDataOf(PROGRESS to transferTotals.transferProgress.floatValue))
            //update the notification
            if (showSingleNotification()) {
                updateProgressNotification(createUpdateNotification(transferTotals, paused))
            }
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

    private val alreadyFinishedGroups = mutableListOf<Int>()
    private suspend fun checkFinishedGroups(transferTotals: ActiveTransferTotals) {
        transferTotals.actionGroups.filter { it.finished() && !alreadyFinishedGroups.contains(it.groupId) }
            .also { finishedGroups ->
                alreadyFinishedGroups.addAll(finishedGroups.map { it.groupId })
                finishedGroups.forEach {
                    showActionGroupFinishedNotification(it)
                }
            }
    }

    private suspend fun checkProgressGroups(
        transferTotals: ActiveTransferTotals,
        paused: Boolean,
    ) {
        transferTotals.actionGroups.filter { alreadyFinishedGroups.contains(it.groupId).not() }
            .onEach {
                showActionGroupProgressNotification(it, paused)
            }
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

        if (loginMutex.isLocked) {
            Timber.d("${this@AbstractTransfersWorker::class.java.simpleName} Login in progress, skipping")
            return@withContext Result.success()
        }

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
                    if (showSingleNotification()) {
                        showFinishNotification(lastActiveTransferTotals)
                    }
                }
                return@withContext Result.success()
            } else {
                if (!storageOverQuota && !transferOverQuota && showSingleNotification()) {
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

    private fun hasAnyPausedChange(
        previous: MonitorOngoingActiveTransfersResult?,
        new: MonitorOngoingActiveTransfersResult,
    ): Boolean {
        if (previous == null || previous.activeTransferTotals.actionGroups.size != new.activeTransferTotals.actionGroups.size) return true
        if (new.paused != previous.paused) return true
        if (new.activeTransferTotals.allPaused() != new.activeTransferTotals.allPaused()) return true

        val previousMap = previous.activeTransferTotals.actionGroups.associateBy { it.groupId }
        return new.activeTransferTotals.actionGroups.any { newGroup ->
            val old = previousMap[newGroup.groupId]
            old == null || newGroup.allPaused() != old.allPaused()
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
        createForegroundInfo(
            if (showSingleNotification()) {
                createUpdateNotification(activeTransferTotals, paused)
            } else {
                createProgressSummaryNotification()
                    ?: createUpdateNotification(activeTransferTotals, paused)
            }
        )

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
        showSingleNotification(
            createFinishNotification(activeTransferTotals),
            finalNotificationId,
        )

    private suspend fun showOverQuotaNotification(storageOverQuota: Boolean) =
        showSingleNotification(
            overQuotaNotificationBuilder(storageOverQuota = storageOverQuota),
            NOTIFICATION_STORAGE_OVERQUOTA
        )

    @SuppressLint("MissingPermission")
    private suspend fun showSingleNotification(
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
     * If true, the notifications for transfers will be shown by each group instead of a single notification for all transfers.
     */
    open suspend fun showGroupedNotifications() = false

    /**
     * Shows a single notification if [showGroupedNotifications] returns false.
     */
    private suspend fun showSingleNotification() = !showGroupedNotifications()

    /**
     * Groups finish notifications by type.
     */
    open suspend fun createFinishSummaryNotification(): Notification? = null

    /**
     * Groups progress notifications by type.
     */
    open suspend fun createProgressSummaryNotification(): Notification? = null

    /**
     * Creates a group finish notification.
     */
    open suspend fun createActionGroupFinishNotification(actionGroup: ActiveTransferTotals.ActionGroup): Notification? =
        null

    @SuppressLint("MissingPermission")
    private suspend fun showActionGroupFinishedNotification(
        actionGroup: ActiveTransferTotals.ActionGroup,
    ) {
        if (areNotificationsEnabledUseCase()) {
            notificationManager.cancel(NOTIFICATION_GROUP_MULTIPLAYER * updateNotificationId + actionGroup.groupId)
            finalNotificationId?.let { finalNotificationId ->
                createFinishSummaryNotification()?.let { summaryNotification ->
                    createActionGroupFinishNotification(actionGroup)?.let { groupNotification ->
                        val groupNotificationId =
                            NOTIFICATION_GROUP_MULTIPLAYER * finalNotificationId + actionGroup.groupId
                        notificationManager.notify(
                            finalNotificationId,
                            summaryNotification,
                        )
                        notificationManager.notify(
                            groupNotificationId,
                            groupNotification,
                        )
                    }
                }
            }
        }
    }

    /**
     * Creates a group progress notification.
     */
    open suspend fun createActionGroupProgressNotification(
        actionGroup: ActiveTransferTotals.ActionGroup,
        paused: Boolean,
    ): Notification? = null

    @SuppressLint("MissingPermission")
    private suspend fun showActionGroupProgressNotification(
        actionGroup: ActiveTransferTotals.ActionGroup,
        paused: Boolean,
    ) {
        if (areNotificationsEnabledUseCase()) {
            createProgressSummaryNotification()?.let { summaryNotification ->
                createActionGroupProgressNotification(
                    actionGroup,
                    paused,
                )?.let { groupNotification ->
                    val groupNotificationId =
                        NOTIFICATION_GROUP_MULTIPLAYER * updateNotificationId + actionGroup.groupId
                    notificationManager.notify(
                        updateNotificationId,
                        summaryNotification,
                    )
                    notificationManager.notify(
                        groupNotificationId,
                        groupNotification,
                    )
                }
            }
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

        /**
         * Final summary group
         */
        private const val FINAL_SUMMARY_GROUP = "FinalSummary"

        /**
         * Final summary group
         */
        fun finalSummaryGroup(transferType: TransferType) = FINAL_SUMMARY_GROUP + transferType.name

        /**
         * Progress summary group
         */
        const val PROGRESS_SUMMARY_GROUP = "ProgressSummary"

        private const val NOTIFICATION_STORAGE_OVERQUOTA = 14

        /**
         * Multiplier for group notifications to be sure that group notifications don't collide
         */
        internal const val NOTIFICATION_GROUP_MULTIPLAYER = 1_000_000

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
