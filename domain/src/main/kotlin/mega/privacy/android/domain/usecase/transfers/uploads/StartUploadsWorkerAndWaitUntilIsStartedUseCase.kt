package mega.privacy.android.domain.usecase.transfers.uploads

import kotlinx.coroutines.flow.first
import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Use case to start uploads worker and wait until is started or already finished (success or failure).
 */
class StartUploadsWorkerAndWaitUntilIsStartedUseCase @Inject constructor(
    private val startUploadsWorkerUseCase: StartUploadsWorkerUseCase,
    private val transferRepository: TransferRepository,
) {

    /**
     * Invoke.
     */
    suspend operator fun invoke() {
        startUploadsWorkerUseCase()
        transferRepository.isUploadsWorkerEnqueuedFlow().first { !it }
    }
}