package mega.privacy.android.domain.usecase.transfers

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * The use case interface to cancel transfer by Tag
 */
class CancelTransferByTagUseCase @Inject constructor(private val transferRepository: TransferRepository) {
    /**
     * Invoke
     * @param transferTag
     */
    suspend operator fun invoke(transferTag: Int) =
        transferRepository.cancelTransferByTag(transferTag)
}
