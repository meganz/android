package mega.privacy.android.domain.usecase.transfers

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Get transfer by uniqueId use case
 *
 * @property transferRepository
 */
class GetTransferByUniqueIdUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke(uniqueId: Long) = transferRepository.getTransferByUniqueId(uniqueId)
}