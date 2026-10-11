package mega.privacy.android.domain.usecase.transfers.errorstatus

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Use case to get the transfer in error status current value
 */
class IsTransferInErrorStatusUseCase @Inject constructor(private val transferRepository: TransferRepository) {
    /**
     * Invoke
     */
    operator fun invoke() = transferRepository.monitorTransferInErrorStatus().value
}