package mega.privacy.android.domain.usecase.transfer

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Use case for checking if there are ongoing transfers.
 */
class OngoingTransfersExistUseCase @Inject constructor(private val transferRepository: TransferRepository) {

    /**
     * Invoke.
     *
     * @return True if there are ongoing transfers, false otherwise.
     */
    suspend operator fun invoke() = transferRepository.ongoingTransfersExist()
}