package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Use case for checking if the queue of pending upload transfers is paused, or if all in progress
 * upload transfers are paused individually.
 */
class AreAllUploadTransfersPausedUseCase @Inject constructor(private val transferRepository: TransferRepository) {

    /**
     * Invokes the use case
     *
     * @return True if all pending upload Transfers are paused, and False if otherwise
     */
    suspend operator fun invoke() = transferRepository.areAllUploadTransfersPaused()
}
