package mega.privacy.android.domain.usecase.transfers

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Get in progress transfers from sdk use case
 *
 * @property repository
 */
class GetInProgressTransfersFromSdkUseCase @Inject constructor(
    private val repository: TransferRepository,
) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke() = repository.getInProgressTransfersFromSdk()
}