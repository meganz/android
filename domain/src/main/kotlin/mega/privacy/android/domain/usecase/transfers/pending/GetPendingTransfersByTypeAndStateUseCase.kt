package mega.privacy.android.domain.usecase.transfers.pending

import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.pending.PendingTransferState
import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Use case to get a flow of pending transfers by type and state
 */
class GetPendingTransfersByTypeAndStateUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {
    /**
     * Invoke
     * @param transferType
     * @param pendingTransferState
     */
    operator fun invoke(
        transferType: TransferType,
        pendingTransferState: PendingTransferState,
    ) = transferRepository.getPendingTransfersByTypeAndState(transferType, pendingTransferState)
}