package mega.privacy.android.domain.entity.transfer

/**
 * Represents an active transfers
 *
 *
 * @param tag An integer that identifies this transfer.
 * @param transferType [TransferType] of this transfer.
 * @param totalBytes the total amount of bytes that will be transferred
 * @param transferredBytes the current amount of bytes already transferred
 */
data class ActiveTransfer(
    val tag: Int,
    val transferType: TransferType,
    val totalBytes: Long,
    val transferredBytes: Long,
)