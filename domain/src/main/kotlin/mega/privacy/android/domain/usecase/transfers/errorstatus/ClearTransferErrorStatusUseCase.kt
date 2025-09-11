package mega.privacy.android.domain.usecase.transfers.errorstatus

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Use case to set the transfer error status back to false once the user has seen it (usually in transfer section)
 */
class ClearTransferErrorStatusUseCase @Inject constructor(private val transferRepository: TransferRepository) {
    /**
     * Invoke
     */
    operator fun invoke() {
        transferRepository.clearTransferErrorStatus()
    }
}