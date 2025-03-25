package mega.privacy.android.feature.sync.data

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.sync.Mutex
import mega.privacy.android.domain.entity.BatteryInfo
import mega.privacy.android.domain.qualifier.LoginMutex
import mega.privacy.android.domain.usecase.IsOnWifiNetworkUseCase
import mega.privacy.android.domain.usecase.environment.MonitorBatteryInfoUseCase
import mega.privacy.android.domain.usecase.login.BackgroundFastLoginUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.StalledIssue
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationMessage
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import mega.privacy.android.feature.sync.domain.usecase.notifcation.GetSyncNotificationUseCase
import mega.privacy.android.feature.sync.domain.usecase.notifcation.SetSyncNotificationShownUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.MonitorSyncStalledIssuesUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.MonitorSyncsUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.PauseResumeSyncsBasedOnBatteryAndWiFiUseCase.Companion.LOW_BATTERY_LEVEL
import mega.privacy.android.feature.sync.domain.usecase.sync.option.MonitorSyncByWiFiUseCase
import mega.privacy.android.feature.sync.ui.notification.SyncNotificationManager
import mega.privacy.android.feature.sync.ui.permissions.SyncPermissionsManager
import timber.log.Timber

/**
 * Sync Worker designed to run the Sync process periodically in the background
 * when the app is closed.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val monitorSyncsUseCase: MonitorSyncsUseCase,
    @LoginMutex private val loginMutex: Mutex,
    private val backgroundFastLoginUseCase: BackgroundFastLoginUseCase,
    private val monitorSyncStalledIssuesUseCase: MonitorSyncStalledIssuesUseCase,
    private val monitorBatteryInfoUseCase: MonitorBatteryInfoUseCase,
    private val monitorSyncByWiFiUseCase: MonitorSyncByWiFiUseCase,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val getSyncNotificationUseCase: GetSyncNotificationUseCase,
    private val isOnWifiNetworkUseCase: IsOnWifiNetworkUseCase,
    private val syncNotificationManager: SyncNotificationManager,
    private val setSyncNotificationShownUseCase: SetSyncNotificationShownUseCase,
    private val syncPermissionManager: SyncPermissionsManager,
) : CoroutineWorker(context, workerParams) {

    private var monitorNotificationsJob: Job? = null

    override suspend fun doWork(): Result {
        Timber.d("SyncWorker started")
        if (!loginMutex.isLocked) {
            runCatching { backgroundFastLoginUseCase() }.getOrElse(Timber::e)
        }
        monitorNotifications()
        while (true) {
            delay(SYNC_WORKER_RECHECK_DELAY)
            val syncs = monitorSyncsUseCase().first()
            Timber.d("SyncWorker syncs: $syncs")
            if (isSyncingCompleted(syncs)) {
                Timber.d("SyncWorker finished, syncs: $syncs")
                return Result.success()
            }
        }
    }

    private fun isSyncingCompleted(syncs: List<FolderPair>): Boolean {
        return syncs.all { it.syncStatus == SyncStatus.SYNCED || it.syncStatus == SyncStatus.PAUSED }
    }

    private suspend fun monitorNotifications() = coroutineScope {
        if (monitorNotificationsJob == null || monitorNotificationsJob?.isCancelled == true) {
            monitorNotificationsJob = combine(
                monitorSyncStalledIssuesUseCase(),
                monitorSyncsUseCase(),
                monitorBatteryInfoUseCase(),
                monitorSyncByWiFiUseCase(),
                monitorConnectivityUseCase()
            ) { stalledIssues: List<StalledIssue>, syncs: List<FolderPair>, batteryInfo: BatteryInfo, syncByWifi: Boolean, _ ->
                runCatching {
                    getSyncNotificationUseCase(
                        isBatteryLow = batteryInfo.level < LOW_BATTERY_LEVEL && !batteryInfo.isCharging,
                        isUserOnWifi = isOnWifiNetworkUseCase(),
                        isSyncOnlyByWifi = syncByWifi,
                        syncs = syncs,
                        stalledIssues = stalledIssues
                    )
                }.onSuccess { notification ->
                    displayNotification(notification)
                }
                    .onFailure {
                        Timber.e(it)
                    }
            }
                .distinctUntilChanged()
                .launchIn(this)
        }
    }

    private suspend fun displayNotification(notification: SyncNotificationMessage?) {
        notification?.let {
            if (syncPermissionManager.isNotificationsPermissionGranted()) {
                var notificationId: Int? = null
                if (!syncNotificationManager.isSyncNotificationDisplayed()) {
                    notificationId = syncNotificationManager.show(context, notification)
                }
                setSyncNotificationShownUseCase(
                    syncNotificationMessage = notification,
                    notificationId = notificationId,
                )
            }
        }
    }

    companion object {
        /**
         * Tag identifying the worker when enqueued
         *
         */
        const val SYNC_WORKER_TAG = "SYNC_WORKER_TAG"

        /**
         * Delay for to check whether the syncs are finished
         */
        const val SYNC_WORKER_RECHECK_DELAY = 6000L
    }
}