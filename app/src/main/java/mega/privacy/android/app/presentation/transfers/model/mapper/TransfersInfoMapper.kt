package mega.privacy.android.app.presentation.transfers.model.mapper

import mega.privacy.android.shared.original.core.ui.model.TransfersInfo
import mega.privacy.android.shared.original.core.ui.model.TransfersStatus
import javax.inject.Inject

/**
 * Maps transfers data to [TransfersInfo] to be used in the presentation layer
 */
class TransfersInfoMapper @Inject constructor() {
    /**
     * Maps transfers data to [TransfersInfo] to be used in the presentation layer
     *
     * @param lastTransfersCancelled true if all the pending transfers have been cancelled, that is: last time the pending transfers turned 0 was because they were cancelled
     */
    operator fun invoke(
        numPendingUploads: Int,
        numPendingDownloadsNonBackground: Int,
        totalSizeToTransfer: Long,
        totalSizeTransferred: Long,
        areTransfersPaused: Boolean,
        isTransferError: Boolean,
        isTransferOverQuota: Boolean,
        isStorageOverQuota: Boolean,
        lastTransfersCancelled: Boolean,
    ): TransfersInfo {
        if (numPendingUploads + numPendingDownloadsNonBackground <= 0) {
            return TransfersInfo(
                when {
                    isTransferError -> TransfersStatus.TransferError
                    lastTransfersCancelled -> TransfersStatus.Cancelled
                    else -> TransfersStatus.Completed
                }
            )
        }
        val pendingDownloads = numPendingDownloadsNonBackground > 0
        val pendingUploads = numPendingUploads > 0

        val uploading = numPendingDownloadsNonBackground <= numPendingUploads

        val status = when {
            areTransfersPaused -> TransfersStatus.Paused
            (isTransferOverQuota && (!pendingUploads || isStorageOverQuota))
                    || (isStorageOverQuota && !pendingDownloads) -> TransfersStatus.OverQuota

            isTransferError -> TransfersStatus.TransferError
            else -> TransfersStatus.Transferring
        }
        return TransfersInfo(
            totalSizeAlreadyTransferred = totalSizeTransferred,
            totalSizeToTransfer = totalSizeToTransfer,
            uploading = uploading,
            status = status,
        )
    }
}