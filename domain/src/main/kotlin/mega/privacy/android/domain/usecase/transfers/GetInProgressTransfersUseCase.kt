package mega.privacy.android.domain.usecase.transfers

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Get in progress transfers use case
 *
 * @property repository
 */
class GetInProgressTransfersUseCase @Inject constructor(
    private val repository: TransferRepository,
) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke() = repository.getInProgressTransfers()
}