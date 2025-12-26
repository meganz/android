package mega.privacy.android.feature.sync.data

import android.app.ForegroundServiceStartNotAllowedException
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import mega.privacy.android.data.worker.ForegroundSetter
import mega.privacy.android.domain.qualifier.LoginMutex
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.login.BackgroundFastLoginUseCase
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationMessage
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import mega.privacy.android.feature.sync.domain.usecase.notifcation.MonitorSyncNotificationsUseCase
import mega.privacy.android.feature.sync.domain.usecase.notifcation.SetSyncNotificationShownUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.GetSyncWorkerForegroundPreferenceUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.MonitorSyncsUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.PauseResumeSyncsBasedOnBatteryAndWiFiUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.SetSyncWorkerForegroundPreferenceUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.MonitorShouldSyncUseCase
import mega.privacy.android.feature.sync.ui.notification.SyncNotificationManager
import mega.privacy.android.feature.sync.ui.permissions.SyncPermissionsManager
import timber.log.Timber
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Sync Worker designed to run the Sync process periodically in the background
 * when the app is closed.
 */
@HiltWorker
internal class SyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val monitorSyncsUseCase: MonitorSyncsUseCase,
    @LoginMutex private val loginMutex: Mutex,
    private val backgroundFastLoginUseCase: BackgroundFastLoginUseCase,
    private val monitorShouldSyncUseCase: MonitorShouldSyncUseCase,
    private val monitorSyncNotificationsUseCase: MonitorSyncNotificationsUseCase,
    private val syncNotificationManager: SyncNotificationManager,
    private val setSyncNotificationShownUseCase: SetSyncNotificationShownUseCase,
    private val pauseResumeSyncsBasedOnBatteryAndWiFiUseCase: PauseResumeSyncsBasedOnBatteryAndWiFiUseCase,
    private val isRootNodeExistsUseCase: RootNodeExistsUseCase,
    private val syncPermissionManager: SyncPermissionsManager,
    private val setSyncWorkerForegroundPreferenceUseCase: SetSyncWorkerForegroundPreferenceUseCase,
    private val getSyncWorkerForegroundPreferenceUseCase: GetSyncWorkerForegroundPreferenceUseCase,
    private val foregroundSetter: ForegroundSetter? = null,
) : CoroutineWorker(context, workerParams) {

    private var monitorNotificationsJob: Job? = null
    private var syncs: List<FolderPair> = emptyList()

    override suspend fun doWork(): Result = coroutineScope {
        Timber.d("SyncWorker started")
        runCatching {
            val isForeground = tryPromoteToForeground()
            if (isLoginSuccessful()) {
                val timeoutDuration = if (isForeground) {
                    MAX_FOREGROUND_DURATION_IN_HOURS.hours
                } else {
                    MAX_BACKGROUND_DURATION_IN_MINUTES.minutes
                }

                monitorNotificationsJob = monitorNotifications()
                val result = withTimeoutOrNull(timeoutDuration) {
                    checkSyncStatus()
                }

                return@coroutineScope if (result == null) {
                    // Timeout occurred
                    Timber.d("SyncWorker timeout")
                    setSyncWorkerForegroundPreferenceUseCase(true)
                    cancelNotificationJob()
                    Result.retry()
                } else {
                    Timber.d("withTimeoutOrNull returned $result")
                    setSyncWorkerForegroundPreferenceUseCase(false)
                    cancelNotificationJob()
                    Timber.d("SyncWorker finished, result: $result")
                    result
                }
            } else {
                // login failed after few attempts
                Timber.d("Login failed")
                cancelNotificationJob()
                Timber.d("SyncWorker finished")
                return@coroutineScope Result.retry()
            }
        }.getOrElse {
            Timber.e(it)
            cancelNotificationJob()
            return@coroutineScope Result.retry()
        }
    }

    private suspend fun tryPromoteToForeground(): Boolean {
        val canAttemptForeground = syncPermissionManager.isNotificationsPermissionGranted()
                && getSyncWorkerForegroundPreferenceUseCase()

        if (!canAttemptForeground) {
            return false
        }

        val promoted = promoteToForeground()
        if (promoted) {
            Timber.d("SyncWorker running in Foreground")
        }
        return promoted
    }

    private suspend fun CoroutineScope.checkSyncStatus(): Result {
        val job = launch {
            monitorSyncsUseCase().onEach {
                Timber.d("SyncWorker syncs: ${it.map { sync -> sync.syncStatus }}")
                syncs = it
            }.catch {
                Timber.e(it, "monitorSyncsUseCase exception")
            }.launchIn(this)
        }
        // add initial delay before checking the sync status
        delay(1.minutes)
        while (syncs.isEmpty() || isSyncingCompleted(syncs).not()) {
            delay(SYNC_WORKER_RECHECK_DELAY_IN_SECONDS.seconds)
            Timber.d("checking sync status...")
        }
        Timber.d("all syncs completed")
        job.cancelAndJoin()
        return Result.success()
    }

    private suspend fun cancelNotificationJob() {
        monitorNotificationsJob?.cancelAndJoin()
        monitorNotificationsJob = null
        Timber.d("monitorNotificationsJob cancelled")
    }

    private fun isSyncingCompleted(syncs: List<FolderPair>): Boolean =
        syncs.isNotEmpty() && syncs.all { it.syncStatus == SyncStatus.SYNCED || it.syncStatus == SyncStatus.PAUSED }

    private suspend fun promoteToForeground(): Boolean {
        val foregroundInfo = createForegroundInfo()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                foregroundSetter?.setForeground(foregroundInfo) ?: setForeground(foregroundInfo)
                true
            } catch (e: ForegroundServiceStartNotAllowedException) {
                Timber.w(e, "Failed to promote SyncWorker to foreground")
                false
            } catch (e: IllegalStateException) {
                Timber.w(e, "The worker is subject to foreground service restrictions")
                false
            }
        } else {
            foregroundSetter?.setForeground(foregroundInfo) ?: setForeground(foregroundInfo)
            true
        }
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val notification = syncNotificationManager.createForegroundNotification(context)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                SYNC_FOREGROUND_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(
                SYNC_FOREGROUND_NOTIFICATION_ID,
                notification
            )
        }
    }

    /**
     * When the user is not logged in, perform a Complete Fast Login procedure
     *
     * @return [Boolean] true if the login process successful otherwise false
     */
    private suspend fun isLoginSuccessful(): Boolean {
        return runCatching {
            Timber.d("Waiting for the user to complete the Fast Login procedure")

            // arbitrary retry value
            var retry = 3
            while (loginMutex.isLocked && retry > 0) {
                Timber.d("Wait for the login lock to be available")
                delay(1.seconds)
                retry--
            }

            return if (!loginMutex.isLocked) {
                val result = runCatching { backgroundFastLoginUseCase() }.onFailure {
                    Timber.e(it, "performCompleteFastLogin exception")
                }
                Timber.d("Complete Fast Login procedure successful")
                result.isSuccess
            } else {
                isRootNodeExistsUseCase().also { rootNodeExists ->
                    if (rootNodeExists) {
                        Timber.d("Root node exists, no need to perform login")
                    } else {
                        Timber.w("Root node does not exist, login failed in the SyncWorker")
                    }
                }
            }
        }.getOrElse { false }
    }

    private fun CoroutineScope.monitorNotifications() = launch {
        Timber.d("monitorSyncsState started")
        monitorShouldSyncUseCase()
            .onEach {
                Timber.d("monitorShouldSyncUseCase: $it")
                withContext(NonCancellable) {
                    pauseResumeSyncsBasedOnBatteryAndWiFiUseCase(it)
                }
            }
            .launchIn(this)

        Timber.d("monitorNotifications started $monitorNotificationsJob")
        monitorSyncNotificationsUseCase()
            .onEach {
                withContext(NonCancellable) {
                    displayNotification(it)
                }
            }
            .launchIn(this)

        Timber.d("monitorNotifications job $monitorNotificationsJob")
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
                Timber.d("displayNotification: ${notification.syncNotificationType}")
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
         * Delay in seconds to check whether the syncs are finished
         */
        const val SYNC_WORKER_RECHECK_DELAY_IN_SECONDS = 30

        /**
         * Max Duration for the sync worker to run
         */
        const val MAX_BACKGROUND_DURATION_IN_MINUTES = 9 // 9 minutes

        const val MAX_FOREGROUND_DURATION_IN_HOURS = 1 // 1 hour

        /**
         * Notification ID for the foreground service
         */
        const val SYNC_FOREGROUND_NOTIFICATION_ID = 123456
    }
}
