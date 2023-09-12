package mega.privacy.android.domain.usecase.transfers

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Delete Failed Or Canceled Transfers Use Case
 *
 */
class DeleteFailedOrCanceledTransfersUseCase @Inject constructor(
    private val transferRepository: TransferRepository
) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke() = transferRepository.deleteFailedOrCanceledTransfers()
}