package mega.privacy.android.domain.usecase.transfer

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Get Failed Or Canceled Transfers Use Case
 *
 */
class GetFailedOrCanceledTransfersUseCase @Inject constructor(
    private val transferRepository: TransferRepository
) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke() = transferRepository.getFailedOrCanceledTransfers()
}