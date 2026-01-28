package mega.privacy.android.domain.usecase.transfers.active

import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.getTransferGroup
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.transfers.GetInProgressTransfersUseCase
import javax.inject.Inject

/**
 * Use case to update active transfers.
 * Active transfers are only stored in memory, so after app re-start they need to be synced with:
 *  - SDK for in progress transfers
 *  - Data base for completed transfers of active transfer groups (user actions)
 */
class UpdateActiveTransfersUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
    private val getInProgressTransfersUseCase: GetInProgressTransfersUseCase,
) {
    /**
     * invoke
     */
    suspend operator fun invoke() {
        val inProgressTransfers: List<Transfer> = getInProgressTransfersUseCase()
        val completedTransfers: List<CompletedTransfer> = transferRepository.getCompletedTransfers()
        val transferGroupIds =
            transferRepository.getActiveTransferGroups().mapNotNull { it.groupId?.toLong() }
        val activeFromCompleted =
            completedTransfers.filter { it.getTransferGroup()?.groupId in transferGroupIds }
        transferRepository.deleteAllActiveTransfers()
        val activeTransfers = inProgressTransfers + activeFromCompleted
        if (activeTransfers.isNotEmpty()) {
            transferRepository.putActiveTransfers(activeTransfers)
        }
    }
}