package mega.privacy.android.domain.usecase.transfers.chatuploads

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Use case to set when transfers have already been requested to be resumed.
 */
class SetAskedResumeTransfersUseCase @Inject constructor(
    private val transfersRepository: TransferRepository,
) {
    /**
     * Invoke.
     */
    suspend operator fun invoke() = transfersRepository.setAskedResumeTransfers()
}