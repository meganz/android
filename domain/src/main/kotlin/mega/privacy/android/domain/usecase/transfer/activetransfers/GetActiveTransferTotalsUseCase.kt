package mega.privacy.android.domain.usecase.transfer.activetransfers

import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Get the active transfer totals of a given [TransferType] from the local database.
 */
class GetActiveTransferTotalsUseCase @Inject constructor(private val transferRepository: TransferRepository) {

    /**
     * Invoke
     * @param transferType [TransferType]
     * @return the [ActiveTransferTotals] of the given type
     */
    suspend operator fun invoke(transferType: TransferType) =
        transferRepository.getCurrentActiveTransferTotalsByType(transferType)
}