package mega.privacy.android.domain.entity.transfer

/**
 * Represents an active transfer.
 */
interface ActiveTransfer : TypedTransfer {

    /**
     * True if the transfer has already finished but it's still part of the current
     */
    val isFinished: Boolean

    /**
     * True if it's a folder transfer, false otherwise (file transfer).
     */
    val isFolderTransfer: Boolean

    /**
     * True if the transfer finished without actually transferring bytes because it was already transferred
     */
    val isAlreadyDownloaded: Boolean
}
