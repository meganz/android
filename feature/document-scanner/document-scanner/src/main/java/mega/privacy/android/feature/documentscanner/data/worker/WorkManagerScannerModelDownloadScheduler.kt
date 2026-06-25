package mega.privacy.android.feature.documentscanner.data.worker

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.Lazy
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
}
