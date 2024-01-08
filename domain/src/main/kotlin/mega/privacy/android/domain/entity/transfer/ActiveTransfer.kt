package mega.privacy.android.domain.entity.transfer

/**
 * Represents an active transfer.
 */
interface ActiveTransfer {

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
     * True if the transfer has already finished but it's still part of the current
     */
    val isFinished: Boolean

    /**
     * True if it's a folder transfer, false otherwise (file transfer).
     */
    val isFolderTransfer: Boolean

    /**
     * True if the transfer is paused, false otherwise
     */
    val isPaused: Boolean

    /**
     * True if the transfer finished without actually transferring bytes because it was already transferred
     */
    val isAlreadyDownloaded: Boolean
}
