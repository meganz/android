package mega.privacy.android.domain.usecase.transfers.downloads

import kotlinx.coroutines.flow.first
import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Use case to start downloads worker and wait until is started or already finished (success or failure).
 */
class StartDownloadsWorkerAndWaitUntilIsStartedUseCase @Inject constructor(
    private val startDownloadWorkerUseCase: StartDownloadWorkerUseCase,
    private val transferRepository: TransferRepository,
) {

    /**
     * Invoke.
     */
    suspend operator fun invoke() {
        startDownloadWorkerUseCase()
        transferRepository.isDownloadsWorkerEnqueuedFlow().first { !it }
    }
}