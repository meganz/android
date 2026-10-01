package mega.privacy.android.domain.usecase.transfers.overquota

import mega.privacy.android.domain.entity.transfer.TransferQuotaState
import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Broadcast the transfer quota state predicted for a batch download.
 */
class BroadcastTransferQuotaStateUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {
    /**
     * Invoke
     *
     * @param state the predicted [TransferQuotaState]
     */
    suspend operator fun invoke(state: TransferQuotaState) =
        transferRepository.broadcastTransferQuotaState(state)
}
