package mega.privacy.android.domain.usecase.transfers.chatuploads

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Start chat uploads worker use case
 *
 * @property transferRepository [TransferRepository].
 */
class StartChatUploadsWorkerUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {

    /**
     * Invoke.
     *
     */
    operator fun invoke() = transferRepository.startChatUploadsWorker()
}