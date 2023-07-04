package mega.privacy.android.domain.usecase.transfer.activetransfers

import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Get a flow of the active transfer totals of a given [TransferType] from the local database.
 */
class MonitorActiveTransferTotalsUseCase @Inject constructor(private val transferRepository: TransferRepository) {

    /**
     * Invoke
     * @param transferType [TransferType]
     * @return a flow of the [ActiveTransferTotals] of the given type
     */
    operator fun invoke(transferType: TransferType) =
        transferRepository.getActiveTransferTotalsByType(transferType)
}