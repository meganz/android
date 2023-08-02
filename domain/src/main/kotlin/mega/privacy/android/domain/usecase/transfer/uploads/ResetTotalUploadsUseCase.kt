package mega.privacy.android.domain.usecase.transfer.uploads

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Use case for resetting the number of uploads.
 *
 * @property isThereAnyPendingUploadsUseCase [IsThereAnyPendingUploadsUseCase]
 * @property transferRepository [TransferRepository]
 */
class ResetTotalUploadsUseCase @Inject constructor(
    private val isThereAnyPendingUploadsUseCase: IsThereAnyPendingUploadsUseCase,
    private val transferRepository: TransferRepository,
) {
    /**
     * Invoke.
     */
    suspend operator fun invoke() {
        if (!isThereAnyPendingUploadsUseCase()) {
            transferRepository.resetTotalUploads()
        }
    }
}