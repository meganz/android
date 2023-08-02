package mega.privacy.android.domain.usecase.transfer

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Pause All Transfers Use Case
 *
 */
class PauseAllTransfersUseCase @Inject constructor(
    private val transferRepository: TransferRepository
) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke(isPause: Boolean) = transferRepository.pauseTransfers(isPause)
}