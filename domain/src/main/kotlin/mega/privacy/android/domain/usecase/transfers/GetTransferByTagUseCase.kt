package mega.privacy.android.domain.usecase.transfers

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Get transfer by tag use case
 *
 * @property transferRepository
 */
class GetTransferByTagUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {
    /**
     * Invoke
     *
     * @param transferTag
     */
    suspend operator fun invoke(transferTag: Int) = transferRepository.getTransferByTag(transferTag)
}