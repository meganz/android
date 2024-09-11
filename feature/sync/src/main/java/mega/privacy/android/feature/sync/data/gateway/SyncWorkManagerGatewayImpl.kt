package mega.privacy.android.feature.sync.data.gateway

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import mega.privacy.android.data.facade.debugWorkInfo
import mega.privacy.android.domain.monitoring.CrashReporter
import mega.privacy.android.feature.sync.data.SyncWorker
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

internal class SyncWorkManagerGatewayImpl @Inject constructor(
    private val workManager: WorkManager,
    private val crashReporter: CrashReporter,
) : SyncWorkManagerGateway {

    override suspend fun enqueueSyncWorkerRequest(
        frequencyInMinutes: Int,
        networkType: NetworkType,
    ) {
        workManager.debugWorkInfo(crashReporter)

        if (!(isWorkerEnqueuedOrRunning())) {
            val workRequest =
                PeriodicWorkRequestBuilder<SyncWorker>(
                    frequencyInMinutes.toLong(),
                    TimeUnit.MINUTES
                )
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(networkType)
                            .setRequiresBatteryNotLow(true)
                            .build()
                    )
                    .addTag(SyncWorker.SYNC_WORKER_TAG)
                    .build()
            workManager.enqueueUniquePeriodicWork(
                SyncWorker.SYNC_WORKER_TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
            Timber.d("SyncWorker is enqueued with frequency: $frequencyInMinutes minutes")
        } else {
            Timber.d("SyncWorker is already running, cannot proceed with additional enqueue request")
        }
    }

    override suspend fun cancelSyncWorkerRequest() {
        workManager.cancelAllWorkByTag(SyncWorker.SYNC_WORKER_TAG)
    }

    /**
     * Check if a worker is currently enqueued or running given its tag
     */
    private fun isWorkerEnqueuedOrRunning(): Boolean {
        return workManager.getWorkInfosByTag(SyncWorker.SYNC_WORKER_TAG).get()
            ?.map { workInfo -> workInfo.state == WorkInfo.State.ENQUEUED || workInfo.state == WorkInfo.State.RUNNING }
            ?.contains(true)
            ?: false
    }
}