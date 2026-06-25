package mega.privacy.android.feature.documentscanner.domain.usecase

import mega.privacy.android.feature.documentscanner.domain.repository.ScannerModelDownloadScheduler
import javax.inject.Inject

/**
 * Starts the background download of the scanner model.
 *
 * @param requireUnmeteredNetwork when true the download waits for an un-metered
 * (Wi-Fi) network — used after the user declines a metered download, so it
 * finishes quietly in the background. When false it runs on any connected
 * network now, which is the path the prepare screen observes.
 */
class StartScannerModelDownloadUseCase @Inject constructor(
    private val scannerModelDownloadScheduler: ScannerModelDownloadScheduler,
) {
    suspend operator fun invoke(requireUnmeteredNetwork: Boolean) =
        scannerModelDownloadScheduler.enqueueModelDownload(requireUnmeteredNetwork)
}
