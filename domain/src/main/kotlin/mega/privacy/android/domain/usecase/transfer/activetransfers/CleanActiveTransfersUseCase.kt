package mega.privacy.android.domain.usecase.transfer.activetransfers

import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.transfer.GetInProgressTransfersUseCase
import javax.inject.Inject

/**
 * Check if there are any not finished not in progress active transfers and removes them from the local database
 * This should not happen, but if for some reason we missed a finish event we need to fix to avoid outdated counters in [ActiveTransferTotals]
 */
class CleanActiveTransfersUseCase @Inject constructor(
    private val getInProgressTransfersUseCase: GetInProgressTransfersUseCase,
    private val transferRepository: TransferRepository,
) {
    /**
     * Invoke.
     * @param transferType the transfer type we want to check
     */
    suspend operator fun invoke(transferType: TransferType) {
        val activeTransfers = transferRepository.getCurrentActiveTransfersByType(transferType)
        val inProgressTags = getInProgressTransfersUseCase().map { it.tag }
        val corruptedTransfersTags = activeTransfers
            .filter { !it.isFinished && !inProgressTags.contains(it.tag) }
            .map { it.tag }
        transferRepository.deleteActiveTransferByTag(corruptedTransfersTags)
    }
}