package mega.privacy.android.domain.entity.transfer

import java.math.BigInteger

/**
 * Data class used as model for MegaTransfer.
 *
 * @property transferType [TransferType]
 * @property transferredBytes Transferred bytes during this transfer.
 * @property totalBytes Total bytes to be transferred to complete the transfer.
 * @property localPath Local path related to this transfer.
 *                      For uploads, this property is the path to the source file.
 *                      For downloads, it is the path of the destination file.
 * @property parentPath The parent path related to this transfer.
 *                       For uploads, this property is the path to the folder containing the source file.
 *                       For downloads, it is that path to the folder containing the destination file.
 * @property nodeHandle Handle related to this transfer.
 *                      For downloads, this property is the handle of the source node.
 *                      For uploads, this property is the handle of the new node in [MEGATransferDelegate onTransferFinish:transfer:error:] and [MEGADelegate onTransferFinish:transfer:error:]
 *                      when the error code is MEGAErrorTypeApiOk, otherwise the value is mega::INVALID_HANDLE.
 * @property parentHandle Handle of the parent node related to this transfer.
 *                        For downloads, this property is mega::INVALID_HANDLE.
 *                        For uploads, it is the handle of the destination node (folder) for the uploaded file.
 * @property fileName  Name of the file that is being transferred.
 *                     It's possible to upload a file with a different name ([MEGASdk startUploadWithLocalPath:parent:]).
 *                     In that case,this property is the destination name.
 * @property stage [TransferStage]
 * @property tag An integer that identifies this transfer.
 * @property folderTransferTag tag of the initial folder transfer that initiated this transfer
 * @property speed The average speed of this transfer.
 * @property isForeignOverQuota True if the transfer has failed with MEGAErrorTypeApiEOverquota
 *                              and the target is foreign, false otherwise.
 * @property isStreamingTransfer True if this is a streaming transfer, false otherwise.
 * @property isFinished True if the transfer is at finished state (completed, cancelled or failed)
 * @property isFolderTransfer True if it's a folder transfer, false otherwise (file transfer).
 * @property appData  The application data associated with this transfer
 * @property appData A list of [TransferAppData] that represents the application data associated with this transfer
 * @property state [TransferState]
 * @property priority Returns the priority of the transfer.
 *                    This value is intended to keep the order of the transfer queue on apps.
 * @property notificationNumber Returns the notification number of the SDK when this MEGATransfer was generated.
 */
data class Transfer(
    override val transferType: TransferType,
    val transferredBytes: Long,
    override val totalBytes: Long,
    val localPath: String,
    val parentPath: String,
    val nodeHandle: Long,
    val parentHandle: Long,
    val fileName: String,
    val stage: TransferStage,
    override val tag: Int,
    val folderTransferTag: Int?,
    val speed: Long,
    val isForeignOverQuota: Boolean,
    val isStreamingTransfer: Boolean,
    override val isFinished: Boolean,
    override val isFolderTransfer: Boolean,
    override val appData: List<TransferAppData>,
    val state: TransferState,
    val priority: BigInteger,
    val notificationNumber: Long,
) : ActiveTransfer, AppDataOwner {

    /**
     * Gets paused state from [state]
     */
    override val isPaused get() = state == TransferState.STATE_PAUSED

    /**
     * true if represents a transfer initiated by the app false if the transfer was initiated by the sdk for children nodes of a folder transfer
     */
    val isRootTransfer = folderTransferTag == null
}
