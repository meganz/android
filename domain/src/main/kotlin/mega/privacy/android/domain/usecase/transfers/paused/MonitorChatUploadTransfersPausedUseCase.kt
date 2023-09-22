package mega.privacy.android.domain.usecase.transfers.paused

import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Use case to get a flow that determines whether the transfer queue is paused or if all individual Chat Upload transfers in progress are paused.
 */
class MonitorChatUploadTransfersPausedUseCase @Inject constructor(
    override val transferRepository: TransferRepository,
) : MonitorTypeTransfersPausedUseCase() {

    override fun isCorrectType(transfer: Transfer) =
        transfer.transferType == TransferType.CHAT_UPLOAD

    override suspend fun totalPendingIndividualTransfers() =
        transferRepository.getNumPendingChatUploads()

    override suspend fun totalPausedIndividualTransfers() =
        transferRepository.getNumPendingPausedChatUploads()
}