package mega.privacy.android.domain.usecase.transfers

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Broadcast failed transfer
 *
 */
class BroadcastFailedTransferUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke(isFailed: Boolean) =
        transferRepository.broadcastFailedTransfer(isFailed)
}