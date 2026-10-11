package mega.privacy.android.domain.usecase.transfers.pending

import mega.privacy.android.domain.entity.transfer.pending.PendingTransfer
import mega.privacy.android.domain.entity.transfer.pending.UpdateAlreadyTransferredFilesCount
import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Use case to update the started files and already transferred files of a [PendingTransfer]
 */
class UpdatePendingTransferStartedCountUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {
    /**
     * Invoke
     * @param pendingTransfer the [PendingTransfer] to be updated
     * @param startedFiles
     * @param alreadyTransferred
     */
    suspend operator fun invoke(
        pendingTransfer: PendingTransfer,
        startedFiles: Int,
        alreadyTransferred: Int,
    ) = transferRepository.updatePendingTransfer(
        UpdateAlreadyTransferredFilesCount(
            pendingTransfer.pendingTransferId,
            startedFiles,
            alreadyTransferred
        )
    )
}