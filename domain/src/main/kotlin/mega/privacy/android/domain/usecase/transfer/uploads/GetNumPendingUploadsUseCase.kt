package mega.privacy.android.domain.usecase.transfer.uploads

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Use case for getting the number of pending uploads.
 */
class GetNumPendingUploadsUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {

    /**
     * Invoke.
     *
     * @return The number of pending downloads  that are not background transfers.
     */
    suspend operator fun invoke() = transferRepository.getNumPendingUploads()
}