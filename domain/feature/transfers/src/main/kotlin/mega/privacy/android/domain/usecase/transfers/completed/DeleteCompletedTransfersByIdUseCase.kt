package mega.privacy.android.domain.usecase.transfers.completed

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Delete Completed Transfers by id Use Case
 *
 */
class DeleteCompletedTransfersByIdUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke(ids: List<Int>) =
        transferRepository.deleteCompletedTransfersById(ids)
}