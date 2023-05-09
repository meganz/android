package mega.privacy.android.domain.usecase.transfer

import mega.privacy.android.domain.entity.transfer.TransfersFinishedState
import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Use case for monitoring transfers finished.
 */
class MonitorTransfersFinishedUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {

    /**
     * Invoke
     *
     * @return Flow [TransfersFinishedState].
     */
    operator fun invoke() = transferRepository.monitorTransfersFinished()
}