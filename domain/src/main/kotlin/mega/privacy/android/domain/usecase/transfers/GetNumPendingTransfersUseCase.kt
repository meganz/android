package mega.privacy.android.domain.usecase.transfers

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Use case for getting the number of pending transfers.
 * For downloads only takes into account those which are not background ones.
 */
class GetNumPendingTransfersUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {

    /**
     * Invoke.
     *
     * @return Number of pending transfers.
     */
    suspend operator fun invoke() = transferRepository.getNumPendingTransfers()
}