package mega.privacy.android.app.domain.entity

/**
 * Transfers info.
 *
 * @property numPendingDownloadsNonBackground   Number of pending downloads that are not background ones.
 * @property numPendingUploads                  Number of pending uploads.
 * @property numPendingTransfers                Number of pending transfers.
 *                                              Sum of [numPendingDownloadsNonBackground] and [numPendingUploads]
 */
data class TransfersInfo(
    val numPendingDownloadsNonBackground: Int,
    val numPendingUploads: Int,
    val numPendingTransfers: Int = numPendingDownloadsNonBackground + numPendingUploads,
)
