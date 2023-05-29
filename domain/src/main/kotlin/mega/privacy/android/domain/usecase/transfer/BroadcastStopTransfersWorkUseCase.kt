package mega.privacy.android.domain.usecase.transfer

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Use case for broadcasting when transfers management have to stop.
 */
class BroadcastStopTransfersWorkUseCase @Inject constructor(private val transferRepository: TransferRepository) {

    /**
     * Invoke.
     */
    suspend operator fun invoke() = transferRepository.broadcastStopTransfersWork()
}