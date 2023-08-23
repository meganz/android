package mega.privacy.android.domain.usecase.transfer.monitorpaused

import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Use case to get a flow that determines whether the transfer queue is paused or if all individual Camera Upload transfers in progress are paused.
 */
class MonitorCameraUploadTransfersPausedUseCase @Inject constructor(
    override val transferRepository: TransferRepository,
) : MonitorTypeTransfersPausedUseCase() {

    override fun isCorrectType(transfer: Transfer) =
        transfer.transferType == TransferType.TYPE_UPLOAD && transfer.isCUUpload()

    override suspend fun totalPendingIndividualTransfers() =
        transferRepository.getNumPendingCameraUploads()

    override suspend fun totalPausedIndividualTransfers() =
        transferRepository.getNumPendingPausedCameraUploads()
}