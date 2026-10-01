package mega.privacy.android.domain.usecase.transfers.completed

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Delete Completed Transfers Use Case
 *
 */
class DeleteCompletedTransfersUseCase @Inject constructor(
    private val transferRepository: TransferRepository
) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke() = transferRepository.deleteCompletedTransfers()
}