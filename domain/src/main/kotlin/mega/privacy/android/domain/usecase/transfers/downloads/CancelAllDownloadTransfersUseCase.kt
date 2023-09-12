package mega.privacy.android.domain.usecase.transfers.downloads

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject


/**
 * Use Case to cancel all download transfers
 */
class CancelAllDownloadTransfersUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {

    /**
     * Invoke
     */
    suspend operator fun invoke() = transferRepository.cancelAllDownloadTransfers()
}