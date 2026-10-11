package mega.privacy.android.domain.usecase.transfers.paused

import javax.inject.Inject

/**
 * Use case for checking if transfers should be paused in remote and pause them if so.
 * This use case is required when transfers are resumed automatically from SDK side, as they do not
 * store this info, but we do.
 */
class CheckIfTransfersShouldBePausedUseCase @Inject constructor(
    private val areTransfersPausedUseCase: AreTransfersPausedUseCase,
    private val pauseTransfersQueueUseCase: PauseTransfersQueueUseCase,
) {
    /**
     * Invoke.
     */
    suspend operator fun invoke() {
        if (areTransfersPausedUseCase()) {
            pauseTransfersQueueUseCase(true)
        }
    }
}
