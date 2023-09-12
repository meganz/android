package mega.privacy.android.domain.usecase.transfers.sd

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Delete sd transfer by tag use case
 *
 */
class DeleteSdTransferByTagUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke(tag: Int) =
        transferRepository.deleteSdTransferByTag(tag)
}