package mega.privacy.android.domain.usecase.transfers.completed

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Check if the completed transfer list is empty.
 *
 * @param transferRepository
 */
class IsCompletedTransfersEmptyUseCase @Inject constructor(
    private val transferRepository: TransferRepository
) {

    /**
     * Invoke.
     *
     * @return true if completed transfers is empty, false otherwise.
     */
    suspend operator fun invoke() =
        transferRepository.isCompletedTransfersEmpty()
}
