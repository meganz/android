package mega.privacy.android.domain.usecase.transfer

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Use case for checking if the queue of transfers is paused or if all in progress transfers
 * are paused individually.
 */
class AreAllTransfersPausedUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {
    /**
     * Invoke.
     *
     * @return True if the queue of transfers is paused or if there are in progress transfers and all of them are
     *         individually paused, false otherwise.
     */
    suspend operator fun invoke() = with(transferRepository) {
        val pendingTransfers = getNumPendingTransfers()
        return@with monitorPausedTransfers().value
                || (pendingTransfers > 0 && getNumPendingPausedUploads() + getNumPendingNonBackgroundPausedDownloads() == pendingTransfers)
    }
}
