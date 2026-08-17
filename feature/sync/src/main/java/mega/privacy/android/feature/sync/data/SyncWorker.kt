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
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withTimeoutOrNull
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.core.coroutine.logAndSwallowExceptions
import mega.privacy.android.data.worker.ForegroundSetter
import mega.privacy.android.domain.monitoring.CrashReporter
import mega.privacy.android.domain.usecase.login.BackgroundFastLoginUseCase
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.StalledIssue
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import mega.privacy.android.feature.sync.domain.usecase.sync.GetSyncWorkerForegroundPreferenceUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.MonitorSyncStalledIssuesUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.MonitorSyncsUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.SetSyncWorkerForegroundPreferenceUseCase
import mega.privacy.android.feature.sync.ui.notification.SyncNotificationManager
import mega.privacy.android.shared.sync.ui.permissions.SyncPermissionsManager
import mega.privacy.mobile.analytics.event.SyncWorkerForegroundExecutionStartedEvent
import timber.log.Timber
import kotlin.coroutines.cancellation.CancellationException
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
    private val monitorSyncStalledIssuesUseCase: MonitorSyncStalledIssuesUseCase,
    private val backgroundFastLoginUseCase: BackgroundFastLoginUseCase,
    private val syncNotificationManager: SyncNotificationManager,
    private val syncPermissionManager: SyncPermissionsManager,
    private val setSyncWorkerForegroundPreferenceUseCase: SetSyncWorkerForegroundPreferenceUseCase,
    private val getSyncWorkerForegroundPreferenceUseCase: GetSyncWorkerForegroundPreferenceUseCase,
    private val foregroundSetter: ForegroundSetter? = null,
    private val crashReporter: CrashReporter,
) : CoroutineWorker(context, workerParams) {

    private var syncs: List<FolderPair> = emptyList()

    override suspend fun doWork(): Result = coroutineScope {
        Timber.d("SyncWorker started")
        runCatching {
            val isForeground = tryPromoteToForeground()
            crashReporter.log("${SyncWorker::class.java.simpleName} Started with isForeground: $isForeground")
            if (isLoginSuccessful()) {
                val timeoutDuration = if (isForeground) {
                    MAX_FOREGROUND_DURATION_IN_HOURS.hours
                } else {
                    MAX_BACKGROUND_DURATION_IN_MINUTES.minutes
                }

                val result = withTimeoutOrNull(timeoutDuration) {
                    checkSyncStatus()
                }

                return@coroutineScope if (result == null) {
                    // Timeout occurred
                    Timber.d("SyncWorker timeout")
                    setSyncWorkerForegroundPreferenceUseCase(true)
                    crashReporter.log("${SyncWorker::class.java.simpleName} finished with timeout after $timeoutDuration")
                    Result.retry()
                } else {
                    Timber.d("withTimeoutOrNull returned $result")
                    setSyncWorkerForegroundPreferenceUseCase(false)
                    Timber.d("SyncWorker finished, result: $result")
                    crashReporter.log("${SyncWorker::class.java.simpleName} finished")
                    result
                }
            } else {
                // login failed after few attempts
                Timber.d("Login failed")
                Timber.d("SyncWorker finished")
                crashReporter.log("${SyncWorker::class.java.simpleName} finished with login failure")
                return@coroutineScope Result.retry()
            }
        }.logAndSwallowExceptions()
            .getOrElse {
                crashReporter.log("${SyncWorker::class.java.simpleName} finished with exception: ${it.message}")
                return@coroutineScope Result.retry()
            }
    }

    private suspend fun tryPromoteToForeground(): Boolean {
        val canAttemptForeground = syncPermissionManager.isNotificationsPermissionGranted()
                && getSyncWorkerForegroundPreferenceUseCase()

        if (!canAttemptForeground) {
            return false
        }

        if (!hasActiveSyncWork()) {
            return false
        }

        val promoted = promoteToForeground()
        if (promoted) {
            Timber.d("SyncWorker running in Foreground")
        }
        return promoted
    }

    private suspend fun hasActiveSyncWork(): Boolean {
        return withTimeoutOrNull(ACTIVE_SYNC_STATE_TIMEOUT) {
            val currentSyncs = monitorSyncsUseCase().first()
            val stalledSyncIds = monitorSyncStalledIssuesUseCase()
                .first()
                .map { it.syncId }
                .toSet()
            currentSyncs.any {
                it.syncStatus == SyncStatus.SYNCING && it.id !in stalledSyncIds
            }
        } == true
    }

    private suspend fun CoroutineScope.checkSyncStatus(): Result {
        syncs = emptyList()
        var stalledIssues = emptyList<StalledIssue>()
        val syncJob = monitorSyncsUseCase()
            .onEach {
                Timber.d("SyncWorker syncs: ${it.map { sync -> sync.syncStatus }}")
                syncs = it
            }
            .catch { throwable ->
                Timber.e(throwable, "monitorSyncsUseCase exception")
                throw throwable
            }
            .launchIn(this)
        val stalledIssuesJob = monitorSyncStalledIssuesUseCase()
            .onEach { stalledIssues = it }
            .catch { throwable ->
                Timber.e(throwable, "monitorSyncStalledIssuesUseCase exception")
                throw throwable
            }
            .launchIn(this)

        return try {
            val hasSyncs = withTimeoutOrNull(INITIAL_SYNC_STATE_TIMEOUT) {
                while (syncs.isEmpty()) {
                    delay(SYNC_WORKER_RECHECK_DELAY_IN_SECONDS.seconds)
                }
                true
            } == true

            if (!hasSyncs) {
                Timber.d("No syncs available for SyncWorker")
                Result.success()
            } else {
                while (syncs.isNotEmpty() && !isSyncingCompleted(syncs, stalledIssues)) {
                    delay(SYNC_WORKER_RECHECK_DELAY_IN_SECONDS.seconds)
                    Timber.d("checking sync status...")
                }
                Timber.d("all syncs completed or no longer available")
                Result.success()
            }
        } finally {
            syncJob.cancelAndJoin()
            stalledIssuesJob.cancelAndJoin()
        }
    }

    private fun isSyncingCompleted(
        syncs: List<FolderPair>,
        stalledIssues: List<StalledIssue>,
    ): Boolean {
        val stalledSyncIds = stalledIssues.map { it.syncId }.toSet()
        return syncs.isNotEmpty() && syncs.all {
            it.syncStatus == SyncStatus.SYNCED ||
                it.syncStatus == SyncStatus.PAUSED ||
                it.syncStatus == SyncStatus.ERROR ||
                it.syncStatus == SyncStatus.DISABLED ||
                it.id in stalledSyncIds
        }
    }

    private suspend fun promoteToForeground(): Boolean {
        val foregroundInfo = createForegroundInfo()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                foregroundSetter?.setForeground(foregroundInfo) ?: setForeground(foregroundInfo)
                Analytics.tracker.trackEvent(SyncWorkerForegroundExecutionStartedEvent)
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
            Analytics.tracker.trackEvent(SyncWorkerForegroundExecutionStartedEvent)
            true
        }
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val notification = syncNotificationManager.createForegroundNotification()
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
     * Ensures the worker has a usable session before it inspects sync state.
     *
     * [BackgroundFastLoginUseCase] already serialises on the login mutex and returns the existing
     * session when the root node is present, so no extra guarding is needed here.
     *
     * @return [Boolean] true if the login process successful otherwise false
     */
    private suspend fun isLoginSuccessful(): Boolean =
        try {
            backgroundFastLoginUseCase()
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "performCompleteFastLogin exception")
            false
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

        private val ACTIVE_SYNC_STATE_TIMEOUT = 5.seconds
        private val INITIAL_SYNC_STATE_TIMEOUT = 1.minutes

        /**
         * Notification ID for the foreground service
         */
        const val SYNC_FOREGROUND_NOTIFICATION_ID = 123456
    }
}
