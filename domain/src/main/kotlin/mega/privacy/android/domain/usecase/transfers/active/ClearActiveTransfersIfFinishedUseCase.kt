package mega.privacy.android.domain.usecase.transfers.active

import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Clear all active transfers of a given [TransferType] from the local database if they are all finished.
 */
class ClearActiveTransfersIfFinishedUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
    private val cleanActiveTransfersUseCase: CleanActiveTransfersUseCase,
) {

    /**
     * Invoke.
     * @param transferType the [TransferType] from which all transfers will be removed
     */
    suspend operator fun invoke(transferType: TransferType) {
        // first make sure we don't have any corrupted entity
        cleanActiveTransfersUseCase(transferType)
        // clear all active transfers of this type if all transfers have finished
        val activeTransfers = transferRepository.getCurrentActiveTransfersByType(transferType)
        if (activeTransfers.isNotEmpty() && activeTransfers.all { it.isFinished }) {
            transferRepository.deleteAllActiveTransfersByType(transferType)
        }
    }
}