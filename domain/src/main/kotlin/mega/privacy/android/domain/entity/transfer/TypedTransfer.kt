package mega.privacy.android.domain.entity.transfer

/**
 * Transfer interface used by the presentation layer.
 */
interface TypedTransfer {
    /**
     * The unique identifier of the transfer
     */
    val uniqueId: Long

    /**
     * An integer that identifies this transfer.
     */
    val tag: Int

    /**
     * The total amount of bytes that will be transferred
     */
    val totalBytes: Long

    /**
     * True if the transfer is paused, false otherwise
     */
    val isPaused: Boolean
}