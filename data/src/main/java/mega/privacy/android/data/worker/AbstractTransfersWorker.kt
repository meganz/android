package mega.privacy.android.data.worker

import android.annotation.SuppressLint
import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.data.mapper.transfer.OverQuotaNotificationBuilder
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.usecase.transfers.MonitorTransferEventsUseCase
import mega.privacy.android.domain.usecase.transfers.active.AddOrUpdateActiveTransferUseCase
import mega.privacy.android.domain.usecase.transfers.active.ClearActiveTransfersIfFinishedUseCase
import mega.privacy.android.domain.usecase.transfers.active.CorrectActiveTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.active.GetActiveTransferTotalsUseCase
import mega.privacy.android.domain.usecase.transfers.active.MonitorOngoingActiveTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.paused.AreTransfersPausedUseCase
import timber.log.Timber

/**
 * Abstract CoroutineWorker to share common implementation of transfers workers
 */
abstract class AbstractTransfersWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val type: TransferType,
    private val ioDispatcher: CoroutineDispatcher,
    private val monitorTransferEventsUseCase: MonitorTransferEventsUseCase,
    private val addOrUpdateActiveTransferUseCase: AddOrUpdateActiveTransferUseCase,
    private val monitorOngoingActiveTransfersUseCase: MonitorOngoingActiveTransfersUseCase,
    private val areTransfersPausedUseCase: AreTransfersPausedUseCase,
    private val getActiveTransferTotalsUseCase: GetActiveTransferTotalsUseCase,
    private val overQuotaNotificationBuilder: OverQuotaNotificationBuilder,
    private val notificationManager: NotificationManagerCompat,
    private val areNotificationsEnabledUseCase: AreNotificationsEnabledUseCase,
    private val correctActiveTransfersUseCase: CorrectActiveTransfersUseCase,
    private val clearActiveTransfersIfFinishedUseCase: ClearActiveTransfersIfFinishedUseCase,
) : CoroutineWorker(context, workerParams) {

    /**
     * Update notification id
     */
    abstract val updateNotificationId: Int

    /**
     * Final notification Id, null if there's no final notification
     */
    open val finalNotificationId: Int? = 0

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
     * Event to do extra work on each transfer event
     */
    open suspend fun onTransferEventReceived(event: TransferEvent) {}

    override suspend fun doWork() = coroutineScope {
        Timber.d("${this::class.java.simpleName} Started")
        // Signal to not kill the worker if the app is killed
        setForegroundAsync(getForegroundInfo())

        withContext(ioDispatcher) {
            val monitorJob = monitorTransferEvents(this)
            correctActiveTransfersUseCase(type) //to be sure we haven't missed any event before monitoring them
            monitorOngoingActiveTransfersUseCase(type)
                .catch { Timber.e("${this::class.java.simpleName}error: $it") }
                .onEach { (transferTotals, paused, _) ->
                    //set progress percent as worker progress
                    setProgress(workDataOf(PROGRESS to transferTotals.transferProgress.floatValue))
                    //update the notification
                    notify(createUpdateNotification(transferTotals, paused))
                    Timber.d("${this::class.java.simpleName}${if (paused) "(paused) " else ""} Notification update (${transferTotals.transferProgress.intValue}):${transferTotals.hasOngoingTransfers()}")
                }
                .last().let { (lastActiveTransferTotals, _, overQuota) ->
                    stopService(monitorJob)
                    clearActiveTransfersIfFinishedUseCase(type)
                    if (lastActiveTransferTotals.hasCompleted()) {
                        Timber.d("${this::class.java.simpleName}Finished Successful: $lastActiveTransferTotals")
                        if (lastActiveTransferTotals.totalTransfers > 0) {
                            showFinishNotification(lastActiveTransferTotals)
                        }
                        Result.success()
                    } else {
                        if (overQuota
                            && !ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(
                                Lifecycle.State.STARTED
                            )
                        ) {
                            //the over quota notification is shown if the app is in background (if not a full dialog will be shown in the app)
                            showFinalNotification(
                                overQuotaNotificationBuilder(),
                                NOTIFICATION_STORAGE_OVERQUOTA
                            )
                        } else {
                            showFinishNotification(lastActiveTransferTotals)
                        }
                        Timber.d("${this::class.java.simpleName}finished Failure: $lastActiveTransferTotals")
                        Result.failure()//to retry in the future
                    }
                }
        }
    }

    override suspend fun getForegroundInfo() =
        createForegroundInfo(
            createUpdateNotification(
                getActiveTransferTotalsUseCase(type),
                areTransfersPausedUseCase()
            )
        )

    /**
     * Monitors transfer events and update the related active transfers
     */
    private fun monitorTransferEvents(scope: CoroutineScope) =
        scope.launch(ioDispatcher) {
            monitorTransferEventsUseCase()
                .filter { it.transfer.transferType == type }
                .collect { transferEvent ->
                    onTransferEventReceived(transferEvent)
                    addOrUpdateActiveTransferUseCase(transferEvent)
                }
        }

    private fun stopService(monitorJob: Job) {
        notificationManager.cancel(updateNotificationId)
        monitorJob.cancel()
    }

    @SuppressLint("MissingPermission")
    private suspend fun notify(notification: Notification) {
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
    }
}