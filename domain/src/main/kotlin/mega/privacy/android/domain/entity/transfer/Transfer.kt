package mega.privacy.android.domain.entity.transfer

/**
 * Transfer
 * a mapper model of MegaTransfer
 *
 * @property totalBytes
 * @property transferredBytes
 * @property transferState
 * @property tag
 * @property transferType
 * @property isFinished
 */
data class Transfer(
    val totalBytes: Long,
    val transferredBytes: Long,
    val transferState: TransferState,
    val tag: Int,
    val transferType: TransferType,
    val isFinished: Boolean,
)
