package mega.privacy.android.domain.usecase.transfers.completed

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Delete All Completed Transfers Use Case
 *
 */
class DeleteAllCompletedTransfersUseCase @Inject constructor(
    private val repository: TransferRepository,
) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke() = repository.deleteAllCompletedTransfers()
}