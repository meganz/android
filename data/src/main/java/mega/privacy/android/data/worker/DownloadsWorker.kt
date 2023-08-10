package mega.privacy.android.data.worker

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.data.mapper.transfer.DownloadNotificationMapper
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.transfer.MonitorPausedTransfersUseCase
import mega.privacy.android.domain.usecase.transfer.MonitorTransferEventsUseCase
import mega.privacy.android.domain.usecase.transfer.activetransfers.AddOrUpdateActiveTransferUseCase
import mega.privacy.android.domain.usecase.transfer.activetransfers.GetActiveTransferTotalsUseCase
import mega.privacy.android.domain.usecase.transfer.activetransfers.MonitorActiveTransferTotalsUseCase
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
    private val monitorActiveTransferTotalsUseCase: MonitorActiveTransferTotalsUseCase,
    private val monitorPausedTransfersUseCase: MonitorPausedTransfersUseCase,
    private val getActiveTransferTotalsUseCase: GetActiveTransferTotalsUseCase,
    private val downloadNotificationMapper: DownloadNotificationMapper,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork() = coroutineScope {
        Timber.d("DownloadsWorker Started")
        // Signal to not kill the worker if the app is killed
        setForegroundAsync(getForegroundInfo())

        withContext(ioDispatcher) {
            val monitorJob = monitorTransferEvents(this)
            updateNotificationWhileThereAreActiveTransfers()
                .last().let { lastActiveTransferTotals ->
                    stopService(monitorJob)
                    if (lastActiveTransferTotals.hasCompleted()) {
                        Timber.d("DownloadsWorker Finished Successful: $lastActiveTransferTotals")
                        Result.success()//to retry in the future
                    } else {
                        Timber.d("DownloadsWorker finished Failure: $lastActiveTransferTotals")
                        Result.failure()
                    }
                }
        }
    }

    override suspend fun getForegroundInfo() =
        createForegroundInfo(
            downloadNotificationMapper(
                getActiveTransferTotalsUseCase(TransferType.TYPE_DOWNLOAD),
                monitorPausedTransfersUseCase().first()
            )
        )

    /**
     * Notification manager used to display notifications
     */
    private val notificationManager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    /**
     * Monitors download transfer events and update the related active transfers
     */
    private fun monitorTransferEvents(scope: CoroutineScope) =
        scope.launch(ioDispatcher) {
            monitorTransferEventsUseCase()
                .filter { it.transfer.transferType == TransferType.TYPE_DOWNLOAD }
                .collect {
                    addOrUpdateActiveTransferUseCase(it.transfer)
                }
        }

    private fun stopService(monitorJob: Job) {
        monitorJob.cancel()
        notificationManager.cancel(DOWNLOAD_NOTIFICATION_ID)
    }

    private fun updateNotificationWhileThereAreActiveTransfers() =
        //this will be converted to an use case with its tests in TRAN-195
        monitorActiveTransferTotalsUseCase(TransferType.TYPE_DOWNLOAD)
            .onStart { emit(getActiveTransferTotalsUseCase(TransferType.TYPE_DOWNLOAD)) }
            .catch { Timber.e("DownloadsWorker error: $it") }
            .combine(monitorPausedTransfersUseCase()) { transferTotals, paused ->
                //update the notification
                val notification = downloadNotificationMapper(transferTotals, paused)
                notificationManager.notify(
                    DOWNLOAD_NOTIFICATION_ID,
                    notification,
                )
                transferTotals
            }
            .transformWhile {
                emit(it)
                Timber.d("DownloadsWorker totals updated (${it.progressPercent}):${it.hasOngoingTransfers()} $it")
                it.hasOngoingTransfers()
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