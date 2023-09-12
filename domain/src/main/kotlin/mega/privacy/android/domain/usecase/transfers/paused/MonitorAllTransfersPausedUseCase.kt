package mega.privacy.android.domain.usecase.transfers.paused

import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Use case to get a flow that determines whether the transfer queue is paused or if all individual transfers in progress are paused.
 */
class MonitorAllTransfersPausedUseCase @Inject constructor(
    override val transferRepository: TransferRepository,
) : MonitorTypeTransfersPausedUseCase() {

    override fun isCorrectType(transfer: Transfer) = true
    override suspend fun totalPendingIndividualTransfers() =
        transferRepository.getNumPendingTransfers()

    override suspend fun totalPausedIndividualTransfers() =
        transferRepository.getNumPendingPausedUploads() + transferRepository.getNumPendingNonBackgroundPausedDownloads()
}