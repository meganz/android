package mega.privacy.android.domain.usecase.transfers.active

import mega.privacy.android.domain.entity.transfer.ActiveTransfer
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Get a flow of the active transfers of a given [TransferType] from the local database.
 */
class MonitorActiveTransfersUseCase @Inject constructor(private val transferRepository: TransferRepository) {

    /**
     * Invoke
     * @param transferType [TransferType]
     * @return a flow of list of [ActiveTransfer] of the given type
     */
    operator fun invoke(transferType: TransferType) =
        transferRepository.getActiveTransfersByType(transferType)
}