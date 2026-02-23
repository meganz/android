package mega.privacy.android.domain.usecase.transfers.completed

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Use case to clear cache related to completed transfers.
 */
class ClearCompletedTransfersCacheUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {
    /**
     * Invoke
     */
    operator fun invoke() {
        transferRepository.clearCompletedTransfersCache()
    }
}
