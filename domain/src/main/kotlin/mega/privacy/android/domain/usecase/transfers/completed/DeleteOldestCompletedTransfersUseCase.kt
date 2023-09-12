package mega.privacy.android.domain.usecase.transfers.completed

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Delete oldest completed transfers
 */
class DeleteOldestCompletedTransfersUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {

    /**
     * Invoke.
     */
    suspend operator fun invoke() =
        transferRepository.deleteOldestCompletedTransfers()
}

