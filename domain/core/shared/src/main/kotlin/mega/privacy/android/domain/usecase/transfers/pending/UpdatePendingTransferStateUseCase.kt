package mega.privacy.android.domain.usecase.transfers.pending

import mega.privacy.android.domain.entity.transfer.pending.PendingTransfer
import mega.privacy.android.domain.entity.transfer.pending.PendingTransferState
import mega.privacy.android.domain.entity.transfer.pending.UpdatePendingTransferState
import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Use case to update the state of a [PendingTransfer]
 */
class UpdatePendingTransferStateUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {
    /**
     * Invoke
     * @param pendingTransfers the [PendingTransfer] to be updated
     * @param state
     */
    suspend operator fun invoke(
        pendingTransfers: List<PendingTransfer>,
        state: PendingTransferState,
    ) = transferRepository.updatePendingTransfers(pendingTransfers.map {
        UpdatePendingTransferState(it.pendingTransferId, state)
    })
}