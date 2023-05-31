package mega.privacy.android.domain.usecase.transfer

import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.HasPendingUploads
import javax.inject.Inject

/**
 * Use case for resetting the number of uploads.
 *
 * @property hasPendingUploads [HasPendingUploads]
 * @property transferRepository [TransferRepository]
 */
class ResetTotalUploadsUseCase @Inject constructor(
    private val hasPendingUploads: HasPendingUploads,
    private val transferRepository: TransferRepository,
) {
    /**
     * Invoke.
     */
    suspend operator fun invoke() {
        if (!hasPendingUploads()) {
            transferRepository.resetTotalUploads()
        }
    }
}