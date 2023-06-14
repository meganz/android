package mega.privacy.android.domain.usecase.transfer

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Use case for getting the number of pending download transfers that are not background transfers.
 */
class GetNumPendingDownloadsNonBackgroundUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {

    /**
     * Invoke.
     *
     * @return The number of pending downloads  that are not background transfers.
     */
    suspend operator fun invoke() = transferRepository.getNumPendingDownloadsNonBackground()
}