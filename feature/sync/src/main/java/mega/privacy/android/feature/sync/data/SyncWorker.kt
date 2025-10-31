package mega.privacy.android.feature.sync.data

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
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
import mega.privacy.android.domain.qualifier.LoginMutex
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.login.BackgroundFastLoginUseCase
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationMessage
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import mega.privacy.android.feature.sync.domain.usecase.notifcation.MonitorSyncNotificationsUseCase
import mega.privacy.android.feature.sync.domain.usecase.notifcation.SetSyncNotificationShownUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.MonitorSyncsUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.PauseResumeSyncsBasedOnBatteryAndWiFiUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.MonitorShouldSyncUseCase
import mega.privacy.android.feature.sync.ui.notification.SyncNotificationManager
import mega.privacy.android.feature.sync.ui.permissions.SyncPermissionsManager
import timber.log.Timber
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
) : CoroutineWorker(context, workerParams) {

    private var monitorNotificationsJob: Job? = null
    private var syncs: List<FolderPair> = emptyList()

    override suspend fun doWork(): Result = coroutineScope {
        Timber.d("SyncWorker started")
        if (isLoginSuccessful()) {
            monitorNotificationsJob = monitorNotifications()
            val result = withTimeoutOrNull(MAX_DURATION.minutes) {
                checkSyncStatus()
            } ?: Result.retry()
            Timber.d("withTimeoutOrNull returned $result")
            cancelNotificationJob()
            Timber.d("SyncWorker finished, result: $result")
            return@coroutineScope result
        } else {
            // login failed after few attempts
            Timber.d("Login failed")
            cancelNotificationJob()
            Timber.d("SyncWorker finished")
            return@coroutineScope Result.retry()
        }
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
            delay(SYNC_WORKER_RECHECK_DELAY)
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
         * Delay for to check whether the syncs are finished
         */
        const val SYNC_WORKER_RECHECK_DELAY = 6000L

        /**
         * Max Duration for the sync worker to run
         */
        const val MAX_DURATION = 9 // 9 minutes in milliseconds
    }
}
