package mega.privacy.android.domain.usecase.transfer

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Use case for broadcasting if transfers are paused.
 *
 */
class BroadcastPausedTransfersUseCase @Inject constructor(private val transferRepository: TransferRepository) {

    /**
     * Invoke.
     *
     * @param isPaused true if all transfers are paused
     */
    suspend operator fun invoke(isPaused: Boolean) =
        transferRepository.broadcastPausedTransfers(isPaused)
}
