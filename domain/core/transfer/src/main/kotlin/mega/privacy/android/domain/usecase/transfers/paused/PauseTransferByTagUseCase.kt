package mega.privacy.android.domain.usecase.transfers.paused

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Pause transfer by tag use case
 */
class PauseTransferByTagUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke(tag: Int, isPause: Boolean): Boolean =
        transferRepository.pauseTransferByTag(tag, isPause)
}