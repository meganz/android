package mega.privacy.android.domain.usecase.transfers.completed

import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Delete Completed Transfer Use Case
 */
class DeleteCompletedTransferUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke(completedTransfer: CompletedTransfer, isRemoveCache: Boolean) =
        transferRepository.deleteCompletedTransfer(completedTransfer, isRemoveCache)
}