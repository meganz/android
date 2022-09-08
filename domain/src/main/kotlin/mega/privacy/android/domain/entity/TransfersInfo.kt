package mega.privacy.android.domain.entity

import mega.privacy.android.domain.entity.transfer.TransferType

/**
 * Transfers info.
 *
 * @property transferType                       MegaTransfer type UPLOAD or DOWNLOAD
 * @property numPendingDownloadsNonBackground   Number of pending downloads that are not background ones.
 * @property numPendingUploads                  Number of pending uploads.
 * @property numPendingTransfers                Number of pending transfers.
 *                                              Sum of [numPendingDownloadsNonBackground] and [numPendingUploads]
 * @property areTransfersPaused                 True if queue of transfers is paused or all the
 *                                              in progress transfers are, false otherwise.
 * @property totalSizeTransferred               total size transferred
 * @property totalSizePendingTransfer           total size pending transfer
 */
data class TransfersInfo(
    val transferType: TransferType,
    val numPendingDownloadsNonBackground: Int,
    val numPendingUploads: Int,
    val areTransfersPaused: Boolean,
    val totalSizePendingTransfer: Long,
    val totalSizeTransferred: Long,
) {
    val numPendingTransfers: Int
        get() = numPendingDownloadsNonBackground + numPendingUploads
}
