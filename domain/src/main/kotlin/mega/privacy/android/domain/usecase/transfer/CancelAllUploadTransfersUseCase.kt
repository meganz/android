package mega.privacy.android.domain.usecase.transfer

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Use Case to cancel all upload transfers
 */
class CancelAllUploadTransfersUseCase @Inject constructor(
    private val transferRepository: TransferRepository
){

    /**
     * Cancel all upload transfers
     */
    suspend operator fun invoke() = transferRepository.cancelAllUploadTransfers()
}