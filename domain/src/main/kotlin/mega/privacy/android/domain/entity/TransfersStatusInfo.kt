package mega.privacy.android.domain.entity

/**
 * Global Transfers status info
 *
 * @property totalSizeToTransfer
 * @property totalSizeTransferred
 * @property pendingUploads
 * @property pendingDownloads
 * @property paused
 * @property transferOverQuota
 * @property storageOverQuota
 */
data class TransfersStatusInfo(
    val totalSizeToTransfer: Long = 0,
    val totalSizeTransferred: Long = 0,
    val pendingUploads: Int = 0,
    val pendingDownloads: Int = 0,
    val paused: Boolean = false,
    val transferOverQuota: Boolean = false,
    val storageOverQuota: Boolean = false,
)