package mega.privacy.android.domain.usecase.transfers.chatuploads

import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.transfers.paused.AreTransfersPausedUseCase
import javax.inject.Inject

/**
 * Use case for checking if should ask for resume transfers.
 */
class ShouldAskForResumeTransfersUseCase @Inject constructor(
    private val transfersRepository: TransferRepository,
    private val areTransfersPausedUseCase: AreTransfersPausedUseCase,
) {
    /**
     * Invoke.
     */
    operator fun invoke() =
        !transfersRepository.monitorAskedResumeTransfers().value && areTransfersPausedUseCase()
}