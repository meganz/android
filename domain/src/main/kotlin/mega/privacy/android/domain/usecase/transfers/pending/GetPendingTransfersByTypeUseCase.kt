package mega.privacy.android.domain.usecase.transfers.pending

import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Use case to get a flow of pending transfers by type and state
 */
class GetPendingTransfersByTypeUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {
    /**
     * Invoke
     * @param transferType
     */
    operator fun invoke(transferType: TransferType) =
        transferRepository.getPendingTransfersByType(transferType)
}