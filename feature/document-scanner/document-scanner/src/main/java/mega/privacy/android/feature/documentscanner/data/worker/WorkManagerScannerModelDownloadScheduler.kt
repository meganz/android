package mega.privacy.android.feature.documentscanner.data.worker

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dagger.Lazy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.feature.documentscanner.domain.model.ScannerModelDownloadState
import mega.privacy.android.feature.documentscanner.domain.repository.ScannerModelDownloadScheduler
import javax.inject.Inject

/**
 * Enqueues [ScannerModelDownloadWorker] via WorkManager.
 *
 * The network constraint comes from the user's choice in the confirmation dialog:
 * an immediate download runs on any connected network, while a declined-metered
 * download is deferred to an un-metered network. Both keep the device off a low
 * battery, and both enqueue as unique work with [ExistingWorkPolicy.KEEP] so
 * repeated requests never stack duplicate downloads.
 */
internal class WorkManagerScannerModelDownloadScheduler @Inject constructor(
    private val workManager: Lazy<WorkManager>,
) : ScannerModelDownloadScheduler {

    override suspend fun enqueueModelDownload(requireUnmeteredNetwork: Boolean) {
        val networkType =
            if (requireUnmeteredNetwork) NetworkType.UNMETERED else NetworkType.CONNECTED
        val request = OneTimeWorkRequestBuilder<ScannerModelDownloadWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(networkType)
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()
        workManager.get().enqueueUniqueWork(
            ScannerModelDownloadWorker.UNIQUE_NAME,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    override fun monitorModelDownload(): Flow<ScannerModelDownloadState> =
        workManager.get()
            .getWorkInfosForUniqueWorkFlow(ScannerModelDownloadWorker.UNIQUE_NAME)
            .map { workInfos -> workInfos.firstOrNull().toDownloadState() }

    private fun WorkInfo?.toDownloadState(): ScannerModelDownloadState = when (this?.state) {
        null -> ScannerModelDownloadState.NotStarted

        // runAttemptCount > 0 means a prior attempt failed and WorkManager is
        // backing off before retrying — surface it as "retrying" so the UI shows a
        // reconnecting hint instead of an apparently-reset, empty progress bar.
        WorkInfo.State.ENQUEUED,
        WorkInfo.State.BLOCKED,
            -> if (runAttemptCount > 0) {
            ScannerModelDownloadState.Retrying
        } else {
            ScannerModelDownloadState.Pending
        }

        WorkInfo.State.RUNNING -> {
            val total = progress.getLong(ScannerModelDownloadWorker.KEY_TOTAL_BYTES, 0L)
            val downloaded = progress.getLong(ScannerModelDownloadWorker.KEY_BYTES_DOWNLOADED, 0L)
            when {
                total > 0L -> ScannerModelDownloadState.Downloading(downloaded, total)
                // Re-running after a failed attempt but no bytes reported yet:
                // keep showing "reconnecting" until the download actually resumes.
                runAttemptCount > 0 -> ScannerModelDownloadState.Retrying
                else -> ScannerModelDownloadState.Pending
            }
        }

        WorkInfo.State.SUCCEEDED -> ScannerModelDownloadState.Completed

        WorkInfo.State.FAILED -> {
            val permanent = outputData.getString(ScannerModelDownloadWorker.KEY_FAILURE_REASON) ==
                    ScannerModelDownloadWorker.FAILURE_PERMANENT
            ScannerModelDownloadState.Failed(permanent = permanent)
        }

        // Cancellation is not part of the prepare flow ("Use old scanner" leaves
        // the work running); treat it as a recoverable failure so a retry is offered.
        WorkInfo.State.CANCELLED -> ScannerModelDownloadState.Failed(permanent = false)
    }
}
