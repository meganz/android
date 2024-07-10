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
    ): TransfersInfo {
        if (numPendingUploads + numPendingDownloadsNonBackground <= 0) {
            return TransfersInfo(TransfersStatus.NotTransferring)
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
            status = status
        )
    }
}