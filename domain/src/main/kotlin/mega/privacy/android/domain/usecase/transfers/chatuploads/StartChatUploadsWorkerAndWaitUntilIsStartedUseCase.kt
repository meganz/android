package mega.privacy.android.domain.usecase.transfers.chatuploads

import kotlinx.coroutines.flow.first
import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Use case to start chat uploads worker and wait until is started or already finished (success or failure).
 */
class StartChatUploadsWorkerAndWaitUntilIsStartedUseCase @Inject constructor(
    private val startChatUploadsWorkerUseCase: StartChatUploadsWorkerUseCase,
    private val transferRepository: TransferRepository,
) {

    /**
     * Invoke.
     */
    suspend operator fun invoke() {
        startChatUploadsWorkerUseCase()
        transferRepository.isChatUploadsWorkerEnqueuedFlow().first { !it }
    }
}