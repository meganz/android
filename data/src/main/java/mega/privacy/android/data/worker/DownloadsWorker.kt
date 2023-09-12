package mega.privacy.android.data.worker

import android.annotation.SuppressLint
import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.data.mapper.transfer.DownloadNotificationMapper
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.transfers.MonitorTransferEventsUseCase
import mega.privacy.android.domain.usecase.transfers.active.AddOrUpdateActiveTransferUseCase
import mega.privacy.android.domain.usecase.transfers.active.GetActiveTransferTotalsUseCase
import mega.privacy.android.domain.usecase.transfers.active.MonitorOngoingActiveDownloadTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.paused.MonitorDownloadTransfersPausedUseCase
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
    private val monitorOngoingActiveDownloadTransfersUseCase: MonitorOngoingActiveDownloadTransfersUseCase,
    private val monitorDownloadTransfersPausedUseCase: MonitorDownloadTransfersPausedUseCase,
    private val getActiveTransferTotalsUseCase: GetActiveTransferTotalsUseCase,
    private val downloadNotificationMapper: DownloadNotificationMapper,
    private val notificationManager: NotificationManagerCompat,
    private val areNotificationsEnabledUseCase: AreNotificationsEnabledUseCase,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork() = coroutineScope {
        Timber.d("DownloadsWorker Started")
        // Signal to not kill the worker if the app is killed
        setForegroundAsync(getForegroundInfo())

        withContext(ioDispatcher) {
            val monitorJob = monitorTransferEvents(this)
            monitorOngoingActiveDownloadTransfersUseCase()
                .catch { Timber.e("DownloadsWorker error: $it") }
                .onEach { (transferTotals, paused) ->
                    //update the notification
                    if (areNotificationsEnabledUseCase()) {
                        notify(downloadNotificationMapper(transferTotals, paused))
                    }
                    Timber.d("DownloadsWorker ${if (paused) "(paused) " else ""} Notification update (${transferTotals.progressPercent}):${transferTotals.hasOngoingTransfers()}")
                }
                .last().let { (lastActiveTransferTotals, _) ->
                    stopService(monitorJob)
                    if (lastActiveTransferTotals.hasCompleted()) {
                        Timber.d("DownloadsWorker Finished Successful: $lastActiveTransferTotals")
                        Result.success()
                    } else {
                        Timber.d("DownloadsWorker finished Failure: $lastActiveTransferTotals")
                        Result.failure()//to retry in the future
                    }
                }
        }
    }

    override suspend fun getForegroundInfo() =
        createForegroundInfo(
            downloadNotificationMapper(
                getActiveTransferTotalsUseCase(TransferType.TYPE_DOWNLOAD),
                monitorDownloadTransfersPausedUseCase().first()
            )
        )

    /**
     * Monitors download transfer events and update the related active transfers
     */
    private fun monitorTransferEvents(scope: CoroutineScope) =
        scope.launch(ioDispatcher) {
            monitorTransferEventsUseCase()
                .filter { it.transfer.transferType == TransferType.TYPE_DOWNLOAD }
                .collect {
                    addOrUpdateActiveTransferUseCase(it)
                }
        }

    private fun stopService(monitorJob: Job) {
        monitorJob.cancel()
        notificationManager.cancel(DOWNLOAD_NOTIFICATION_ID)
    }

    @SuppressLint("MissingPermission")
    private fun notify(notification: Notification) {
        notificationManager.notify(
            DOWNLOAD_NOTIFICATION_ID,
            notification,
        )
    }

    /**
     * Create a [ForegroundInfo] based on [Notification]
     */
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
         * Tag for enqueue the worker to work manager
         */
        const val SINGLE_DOWNLOAD_TAG = "MEGA_DOWNLOAD_TAG"
        private const val DOWNLOAD_NOTIFICATION_ID = 2
    }
}