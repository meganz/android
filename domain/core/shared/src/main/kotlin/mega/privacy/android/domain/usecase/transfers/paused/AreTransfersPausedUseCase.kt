package mega.privacy.android.domain.usecase.transfers.paused

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * The use case interface to get paused transfers boolean flag
 */
class AreTransfersPausedUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {
    /**
     * Are transfers paused (downloads and uploads)
     */
    operator fun invoke() = transferRepository.monitorPausedTransfers().value
}
