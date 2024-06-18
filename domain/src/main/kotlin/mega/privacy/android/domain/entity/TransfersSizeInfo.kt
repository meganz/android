package mega.privacy.android.domain.entity

/**
 * Size transfer info
 *
 * @property totalSizeToTransfer
 * @property totalSizeTransferred
 * @property pendingUploads
 * @property pendingDownloads
 */
data class TransfersSizeInfo(
    val totalSizeToTransfer: Long = 0,
    val totalSizeTransferred: Long = 0,
    val pendingUploads: Int? = null,
    val pendingDownloads: Int? = null,
)