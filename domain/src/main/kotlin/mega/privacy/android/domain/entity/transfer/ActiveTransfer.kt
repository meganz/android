package mega.privacy.android.domain.entity.transfer

/**
 * Represents an active transfer.
 */
interface ActiveTransfer : TypedTransfer, AppDataOwner {

    /**
     * [TransferType] of this transfer.
     */
    val transferType: TransferType

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
    val isAlreadyTransferred: Boolean

    /**
     * True if the transfer finished because it was cancelled before ending
     */
    val isCancelled: Boolean

    /**
     * Name of the file that is being transferred.
     */
    val fileName: String

    /**
     * Local path related to this transfer.
     * For uploads, this property is the path to the source file.
     * For downloads, it is the path of the destination file.
     */
    val localPath: String
}
