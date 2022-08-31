package mega.privacy.android.domain.entity

/**
 * Size transfer info
 *
 * @property transferType
 * @property totalSizePendingTransfer
 * @property totalSizeTransferred
 */
data class TransfersSizeInfo(
    val transferType: Int = -1,
    val totalSizePendingTransfer: Long = 0,
    val totalSizeTransferred: Long = 0,
)