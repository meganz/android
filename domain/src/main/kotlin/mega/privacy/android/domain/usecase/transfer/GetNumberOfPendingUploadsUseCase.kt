package mega.privacy.android.domain.usecase.transfer

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Get Number of Pending Uploads
 */
class GetNumberOfPendingUploadsUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {
    /**
     * Invoke
     *
     * @return number of pending uploads
     */
    suspend operator fun invoke() = transferRepository.getNumPendingUploads()
}
