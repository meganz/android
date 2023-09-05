package mega.privacy.android.domain.usecase.transfer

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Get Completed Transfer By Id Use Case
 *
 */
class GetCompletedTransferByIdUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke(id: Int) =
        transferRepository.getCompletedTransferById(id)
}