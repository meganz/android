package mega.privacy.android.domain.usecase.transfers.overquota

import mega.privacy.android.domain.entity.transfer.TransferOverQuotaStatus
import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Broadcast a transfer over quota event (over quota or almost over quota).
 *
 * Unlike [BroadcastTransferOverQuotaUseCase], this emits an event on every occurrence rather than
 * only on state changes.
 */
class BroadcastTransferOverQuotaEventUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {
    /**
     * Invoke
     *
     * @param status the [TransferOverQuotaStatus] to broadcast
     */
    suspend operator fun invoke(status: TransferOverQuotaStatus) =
        transferRepository.broadcastTransferOverQuotaEvent(status)
}
