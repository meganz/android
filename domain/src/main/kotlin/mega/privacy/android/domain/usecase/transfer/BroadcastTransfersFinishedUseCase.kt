package mega.privacy.android.domain.usecase.transfer

import mega.privacy.android.domain.entity.transfer.TransfersFinishedState
import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Use case for broadcasting transfers finished.
 *
 */
class BroadcastTransfersFinishedUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {

    /**
     * Invoke.
     *
     * @param transfersFinishedState [TransfersFinishedState].
     */
    suspend operator fun invoke(transfersFinishedState: TransfersFinishedState) =
        transferRepository.broadcastTransfersFinished(transfersFinishedState)
}