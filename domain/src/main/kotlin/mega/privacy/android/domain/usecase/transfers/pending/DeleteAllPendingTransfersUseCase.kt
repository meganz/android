package mega.privacy.android.domain.usecase.transfers.pending

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Use case to delete all pending transfers
 */
class DeleteAllPendingTransfersUseCase @Inject constructor(
    private val transferRepository: TransferRepository
) {
    /**
     * Invoke
     */
    suspend operator fun invoke() {
        transferRepository.deleteAllPendingTransfers()
    }
}