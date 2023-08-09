package mega.privacy.android.app.transfers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
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
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
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
            createNotification(
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
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun updateNotificationWhileThereAreActiveTransfers() =
        //this will be converted to an use case with its tests in TRAN-195
        monitorActiveTransferTotalsUseCase(TransferType.TYPE_DOWNLOAD)
            .onStart { emit(getActiveTransferTotalsUseCase(TransferType.TYPE_DOWNLOAD)) }
            .catch { Timber.e("DownloadsWorker error: $it") }
            .combine(monitorPausedTransfersUseCase()) { transfer, paused ->
                //update the notification
                notificationManager.notify(NOTIFICATION_ID, createNotification(transfer, paused))
                transfer
            }
            .transformWhile {
                emit(it)
                Timber.d("DownloadsWorker totals updated (${it.progressPercent}):${it.hasOngoingTransfers()} $it")
                it.hasOngoingTransfers()
            }

    /**
     *  Create a [Notification]
     */
    private fun createNotification(
        activeTransferTotals: ActiveTransferTotals?,
        paused: Boolean,
        intent: PendingIntent? = null,
    ): Notification {
        //this will be done at app startup with a usecase, similar to CreateChatNotificationChannelsUseCase in TRAN-197
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.setShowBadge(false)
            channel.setSound(null, null)
            notificationManager.createNotificationChannel(channel)
        }
        val content = context.getString(R.string.download_touch_to_show)
        val title =
            if (activeTransferTotals == null || activeTransferTotals.transferredBytes == 0L) {
                context.getString(R.string.download_preparing_files)
            } else {
                val inProgress = activeTransferTotals.totalFinishedTransfers
                val totalTransfers = activeTransferTotals.totalTransfers
                if (paused) {
                    context.getString(
                        R.string.download_service_notification_paused,
                        inProgress,
                        totalTransfers
                    )
                } else {
                    context.getString(
                        R.string.download_service_notification,
                        inProgress,
                        totalTransfers
                    )
                }
            }
        val subText = activeTransferTotals?.let {
            Util.getProgressSize(
                context,
                activeTransferTotals.transferredBytes,
                activeTransferTotals.totalBytes
            )
        }

        val builder = NotificationCompat.Builder(
            context,
            NOTIFICATION_CHANNEL_ID
        ).apply {
            setSmallIcon(R.drawable.ic_stat_notify)
            setOngoing(true)
            setContentTitle(title)
            setStyle(NotificationCompat.BigTextStyle().bigText(content))
            setContentText(content)
            setOnlyAlertOnce(true)
            setAutoCancel(false)
            intent?.let { setContentIntent(intent) }
            activeTransferTotals?.progressPercent?.let { setProgress(100, it, false) }
            subText?.let { setSubText(subText) }
        }
        return builder.build()
    }

    /**
     * Create a [ForegroundInfo] based on [Notification]
     */
    private fun createForegroundInfo(notification: Notification) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification
            )
        }

    companion object {
        private const val NOTIFICATION_ID = Constants.NOTIFICATION_DOWNLOAD
        private const val NOTIFICATION_CHANNEL_ID = Constants.NOTIFICATION_CHANNEL_DOWNLOAD_ID
        private const val NOTIFICATION_CHANNEL_NAME = Constants.NOTIFICATION_CHANNEL_DOWNLOAD_NAME
    }

}