package mega.privacy.android.domain.usecase.transfers.paused

import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Use case to get a flow that determines whether the transfer queue is paused or if all individual General Upload (not CU or chat) transfers in progress are paused.
 */
class MonitorGeneralUploadTransfersPausedUseCase @Inject constructor(
    override val transferRepository: TransferRepository,
) : MonitorTypeTransfersPausedUseCase() {

    override fun isCorrectType(transfer: Transfer) =
        transfer.transferType == TransferType.TYPE_UPLOAD && !transfer.isChatUpload() && !transfer.isCUUpload()

    override suspend fun totalPendingIndividualTransfers() =
        transferRepository.getNumPendingGeneralUploads()

    override suspend fun totalPausedIndividualTransfers() =
        transferRepository.getNumPendingPausedGeneralUploads()
}