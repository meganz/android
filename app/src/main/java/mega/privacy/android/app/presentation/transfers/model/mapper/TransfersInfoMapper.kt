package mega.privacy.android.app.presentation.transfers.model.mapper

import mega.privacy.android.domain.entity.TransfersInfo
import mega.privacy.android.domain.entity.TransfersStatus
import mega.privacy.android.domain.entity.transfer.TransferType
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
        totalSizePendingTransfer: Long,
        totalSizeTransferred: Long,
        transferType: TransferType,
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

        val uploading = transferType.isUploadType() && pendingUploads
                || (!(transferType == TransferType.DOWNLOAD && pendingDownloads)
                && numPendingDownloadsNonBackground <= numPendingUploads)

        val status = when {
            areTransfersPaused -> TransfersStatus.Paused
            (isTransferOverQuota && (!pendingUploads || isStorageOverQuota))
                    || (isStorageOverQuota && !pendingDownloads) -> TransfersStatus.OverQuota

            isTransferError -> TransfersStatus.TransferError
            else -> TransfersStatus.Transferring
        }
        return TransfersInfo(
            totalSizeTransferred = totalSizeTransferred,
            totalSizePendingTransfer = totalSizePendingTransfer,
            uploading = uploading,
            status = status
        )
    }
}