package mega.privacy.android.domain.usecase.transfer.sd

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Get all sd transfers use case
 *
 */
class GetAllSdTransfersUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke() = transferRepository.getAllSdTransfers()
}