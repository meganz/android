package mega.privacy.android.domain.entity.transfer

/**
 * Represents an active transfers
 *
 *
 * @param tag An integer that identifies this transfer.
 * @param transferType [TransferType] of this transfer.
 * @param totalBytes the total amount of bytes that will be transferred
 * @param transferredBytes the current amount of bytes already transferred
 * @param isFinished true if the transfer has already finished but it's still part of the current
 */
data class ActiveTransfer(
    val tag: Int,
    val transferType: TransferType,
    val totalBytes: Long,
    val transferredBytes: Long,
    val isFinished: Boolean,
)