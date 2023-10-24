package mega.privacy.android.data.worker

import android.annotation.SuppressLint
import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
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
import mega.privacy.android.data.mapper.transfer.DownloadNotificationMapper
import mega.privacy.android.data.mapper.transfer.OverQuotaNotificationBuilder
import mega.privacy.android.data.mapper.transfer.TransfersFinishedNotificationMapper
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.transfers.MonitorTransferEventsUseCase
import mega.privacy.android.domain.usecase.transfers.active.AddOrUpdateActiveTransferUseCase
import mega.privacy.android.domain.usecase.transfers.active.ClearActiveTransfersIfFinishedUseCase
import mega.privacy.android.domain.usecase.transfers.active.CorrectActiveTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.active.GetActiveTransferTotalsUseCase
import mega.privacy.android.domain.usecase.transfers.active.MonitorOngoingActiveTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.paused.AreTransfersPausedUseCase
import timber.log.Timber

/**
 * Worker that will monitor current active transfers while there are some
 * This should be used once the downloads are actually started, it won't start any download.
 */
@HiltWorker
class DownloadsWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val monitorTransferEventsUseCase: MonitorTransferEventsUseCase,
    private val addOrUpdateActiveTransferUseCase: AddOrUpdateActiveTransferUseCase,
    private val monitorOngoingActiveTransfersUseCase: MonitorOngoingActiveTransfersUseCase,
    private val areTransfersPausedUseCase: AreTransfersPausedUseCase,
    private val getActiveTransferTotalsUseCase: GetActiveTransferTotalsUseCase,
    private val downloadNotificationMapper: DownloadNotificationMapper,
    private val transfersFinishedNotificationMapper: TransfersFinishedNotificationMapper,
    private val overQuotaNotificationBuilder: OverQuotaNotificationBuilder,
    private val notificationManager: NotificationManagerCompat,
    private val areNotificationsEnabledUseCase: AreNotificationsEnabledUseCase,
    private val correctActiveTransfersUseCase: CorrectActiveTransfersUseCase,
    private val clearActiveTransfersIfFinishedUseCase: ClearActiveTransfersIfFinishedUseCase,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork() = coroutineScope {
        Timber.d("DownloadsWorker Started")
        // Signal to not kill the worker if the app is killed
        setForegroundAsync(getForegroundInfo())

        withContext(ioDispatcher) {
            val monitorJob = monitorTransferEvents(this)
            correctActiveTransfersUseCase(TransferType.DOWNLOAD) //to be sure we haven't missed any event before monitoring them
            monitorOngoingActiveTransfersUseCase(TransferType.DOWNLOAD)
                .catch { Timber.e("DownloadsWorker error: $it") }
                .onEach { (transferTotals, paused, _) ->
                    //set progress percent as worker progress
                    setProgress(workDataOf(Progress to transferTotals.progressPercent))
                    //update the notification
                    if (areNotificationsEnabledUseCase()) {
                        notify(downloadNotificationMapper(transferTotals, paused))
                    }
                    Timber.d("DownloadsWorker ${if (paused) "(paused) " else ""} Notification update (${transferTotals.progressPercent}):${transferTotals.hasOngoingTransfers()}")
                }
                .last().let { (lastActiveTransferTotals, _, overQuota) ->
                    stopService(monitorJob)
                    clearActiveTransfersIfFinishedUseCase(TransferType.DOWNLOAD)
                    if (lastActiveTransferTotals.hasCompleted()) {
                        Timber.d("DownloadsWorker Finished Successful: $lastActiveTransferTotals")
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
                            showFinalNotification(overQuotaNotificationBuilder())
                        } else {
                            showFinishNotification(lastActiveTransferTotals)
                        }
                        Timber.d("DownloadsWorker finished Failure: $lastActiveTransferTotals")
                        Result.failure()//to retry in the future
                    }
                }
        }
    }

    override suspend fun getForegroundInfo() =
        createForegroundInfo(
            downloadNotificationMapper(
                getActiveTransferTotalsUseCase(TransferType.DOWNLOAD),
                areTransfersPausedUseCase()
            )
        )

    /**
     * Monitors download transfer events and update the related active transfers
     */
    private fun monitorTransferEvents(scope: CoroutineScope) =
        scope.launch(ioDispatcher) {
            monitorTransferEventsUseCase()
                .filter { it.transfer.transferType == TransferType.DOWNLOAD }
                .collect {
                    addOrUpdateActiveTransferUseCase(it)
                }
        }

    private fun stopService(monitorJob: Job) {
        notificationManager.cancel(DOWNLOAD_NOTIFICATION_ID)
        monitorJob.cancel()
    }

    @SuppressLint("MissingPermission")
    private fun notify(notification: Notification) {
        notificationManager.notify(
            DOWNLOAD_NOTIFICATION_ID,
            notification,
        )
    }

    private suspend fun showFinishNotification(activeTransferTotals: ActiveTransferTotals) =
        showFinalNotification(transfersFinishedNotificationMapper(activeTransferTotals))

    @SuppressLint("MissingPermission")
    private fun showFinalNotification(notification: Notification) {
        notificationManager.notify(
            NOTIFICATION_DOWNLOAD_FINAL,
            notification,
        )
    }

    /**
     * Create a [ForegroundInfo] based on [Notification]
     */
    @SuppressLint("SpecifyForegroundServiceType")
    private fun createForegroundInfo(notification: Notification) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                DOWNLOAD_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(
                DOWNLOAD_NOTIFICATION_ID,
                notification
            )
        }


    companion object {
        /**
         * Tag to get the progress percent of this worker
         */
        const val Progress = "Progress"

        /**
         * Tag for enqueue the worker to work manager
         */
        const val SINGLE_DOWNLOAD_TAG = "MEGA_DOWNLOAD_TAG"
        private const val DOWNLOAD_NOTIFICATION_ID = 2
        private const val NOTIFICATION_DOWNLOAD_FINAL = 4
    }
}