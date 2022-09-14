package mega.privacy.android.domain.entity

import mega.privacy.android.domain.entity.transfer.TransferType

/**
 * Size transfer info
 *
 * @property transferType
 * @property totalSizePendingTransfer
 * @property totalSizeTransferred
 */
data class TransfersSizeInfo(
    val transferType: TransferType = TransferType.NONE,
    val totalSizePendingTransfer: Long = 0,
    val totalSizeTransferred: Long = 0,
)