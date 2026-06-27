package mega.privacy.android.feature.documentscanner.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.feature.documentscanner.domain.model.ScannerModelDownloadState
import mega.privacy.android.feature.documentscanner.domain.repository.ScannerModelDownloadScheduler
import javax.inject.Inject

/**
 * Observes the state of the background scanner-model download so the prepare
 * screen can render progress and react to success or failure.
 */
class MonitorScannerModelDownloadUseCase @Inject constructor(
    private val scannerModelDownloadScheduler: ScannerModelDownloadScheduler,
) {
    operator fun invoke(): Flow<ScannerModelDownloadState> =
        scannerModelDownloadScheduler.monitorModelDownload()
}
