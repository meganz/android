package mega.privacy.android.domain.usecase.transfers

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Use case to resume transfers for not logged in instance.
 */
class ResumeTransfersForNotLoggedInInstanceUseCase @Inject constructor(
    private val transfersRepository: TransferRepository,
) {

    /**
     * Invoke.
     */
    suspend operator fun invoke() {
        transfersRepository.resumeTransfersForNotLoggedInInstance()
    }
}