package mega.privacy.android.domain.entity.transfer

/**
 * Represents an active transfer.
 */
interface ActiveTransfer : TypedTransfer {

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
     * [TransferState]
     */
    val state: TransferState

    /**
     * Local path related to this transfer.
     * For uploads, this property is the path to the source file.
     * For downloads, it is the path of the destination file.
     **/
    val localPath: String

    /**
     * Handle related to this transfer.
     * For downloads, this property is the handle of the source node.
     * It's not used for ActiveTransfer uploads as once it has a handle is not active anymore.
     **/
    val nodeHandle: Long
}
