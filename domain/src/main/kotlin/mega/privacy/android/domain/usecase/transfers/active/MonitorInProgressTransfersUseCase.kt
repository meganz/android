package mega.privacy.android.domain.usecase.transfers.active

import mega.privacy.android.domain.entity.transfer.InProgressTransfer
import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Monitor In Progress Transfers Use Case.
 */
class MonitorInProgressTransfersUseCase @Inject constructor(private val transferRepository: TransferRepository) {

    /**
     * Invoke
     *
     * @return a flow of Map. Being the key an [Int] representing the transfer tag and [InProgressTransfer] as its value.
     */
    operator fun invoke() = transferRepository.monitorInProgressTransfers()
}