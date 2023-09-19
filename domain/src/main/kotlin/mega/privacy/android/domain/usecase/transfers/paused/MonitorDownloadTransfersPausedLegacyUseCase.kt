package mega.privacy.android.domain.usecase.transfers.paused

import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Use case to get a flow that determines whether the transfer queue is paused or if all individual Download transfers in progress are paused.
 * @Deprecated, this use case is using sdk calls to get counters, once everything is moved to ActiveTransfers MonitorDownloadTransfersPausedUseCase should be used
 */
class MonitorDownloadTransfersPausedLegacyUseCase @Inject constructor(
    override val transferRepository: TransferRepository,
) : MonitorTypeTransfersPausedUseCase() {

    override fun isCorrectType(transfer: Transfer) =
        transfer.transferType == TransferType.TYPE_DOWNLOAD

    override suspend fun totalPendingIndividualTransfers() =
        transferRepository.getNumPendingDownloadsNonBackground()

    override suspend fun totalPausedIndividualTransfers() =
        transferRepository.getNumPendingNonBackgroundPausedDownloads()
}