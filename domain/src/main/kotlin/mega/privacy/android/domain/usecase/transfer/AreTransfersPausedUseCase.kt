package mega.privacy.android.domain.usecase.transfer

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
    suspend operator fun invoke() = transferRepository.monitorPausedTransfers().value
}
