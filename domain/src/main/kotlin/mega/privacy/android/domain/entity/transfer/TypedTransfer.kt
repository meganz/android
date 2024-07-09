package mega.privacy.android.domain.entity.transfer

/**
 * Transfer interface used by the presentation layer.
 */
interface TypedTransfer {
    /**
     * An integer that identifies this transfer.
     */
    val tag: Int

    /**
     * [TransferType] of this transfer.
     */
    val transferType: TransferType

    /**
     * The total amount of bytes that will be transferred
     */
    val totalBytes: Long

    /**
     * True if the transfer is paused, false otherwise
     */
    val isPaused: Boolean
}