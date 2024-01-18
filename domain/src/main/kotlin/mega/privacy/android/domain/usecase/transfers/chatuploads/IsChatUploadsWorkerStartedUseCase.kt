package mega.privacy.android.domain.usecase.transfers.chatuploads

import kotlinx.coroutines.flow.first
import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Use case to wait until the worker is started or already finished (success or failure), this use case is usually invoked after [StartChatUploadsWorkerUseCase] to wait for its start
 */
class IsChatUploadsWorkerStartedUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {

    /**
     * Invoke function
     * @return It will wait until the worker is not enqueued (it's started or finished)
     */
    suspend operator fun invoke() {
        transferRepository.isChatUploadsWorkerEnqueuedFlow().first { !it }
    }
}