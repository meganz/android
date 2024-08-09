package mega.privacy.android.domain.usecase.transfers.active

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Clear all active transfers from the local database if they are all finished.
 */
class ClearActiveTransfersIfFinishedUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
    private val correctActiveTransfersUseCase: CorrectActiveTransfersUseCase,
) {

    /**
     * Invoke.
     */
    suspend operator fun invoke() {
        // first make sure we don't have any corrupted entity
        correctActiveTransfersUseCase(null)
        // clear all active transfers of this type if all transfers have finished
        val activeTransfers = transferRepository.getCurrentActiveTransfers()
        if (activeTransfers.isNotEmpty() && activeTransfers.all { it.isFinished }) {
            transferRepository.deleteAllActiveTransfers()
        }
    }
}