package mega.privacy.android.domain.entity

/**
 * Transfers info.
 *
 * @property numPendingDownloadsNonBackground   Number of pending downloads that are not background ones.
 * @property numPendingUploads                  Number of pending uploads.
 * @property numPendingTransfers                Number of pending transfers.
 *                                              Sum of [numPendingDownloadsNonBackground] and [numPendingUploads]
 * @property areTransfersPaused                 True if queue of transfers is paused or all the
 *                                              in progress transfers are, false otherwise.
 */
data class TransfersInfo(
    val numPendingDownloadsNonBackground: Int,
    val numPendingUploads: Int,
    val numPendingTransfers: Int = numPendingDownloadsNonBackground + numPendingUploads,
    val areTransfersPaused: Boolean,
)
