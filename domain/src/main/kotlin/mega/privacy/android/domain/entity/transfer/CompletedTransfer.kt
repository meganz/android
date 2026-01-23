package mega.privacy.android.domain.entity.transfer

import java.io.File

/**
 * A representation of a completed transfer
 *
 * @property id to uniquely identify the transfer in local storage
 * @property fileName the name of the file
 * @property type the transfer type (eg. TYPE_DOWNLOAD, TYPE_UPLOAD)
 * @property state the transfer state (eg. STATE_COMPLETED, STATE_CANCELLED, STATE_FAILED)
 * @property size the size of the file
 * @property handle the handle of the node associated to the transfer
 * @property path the path of the node in case of a transfer to the cloud, the destination path in case of a transfer to SD card
 * @property displayPath if path represents a content:// uri, this represents the path to display to the user if it's possible to get
 * @property isOffline true if the file is available offline
 * @property error the String representing the errorCode, if [errorCode] is not null is the raw string returned by the SDK
 * @property errorCode the int representing the errorCode returned by API
 * @property originalPath the original local path of the file transferred
 * @property parentHandle  the parent handle of the file transferred
 * @property timestamp represents the time when the transfer is completed
 * @property appData The application data associated with this transfer
 * @property uniqueId The unique identifier of the transfer
 * @property totalBytes Total bytes of the transfer
 */
data class CompletedTransfer(
    val id: Int? = null,
    override val fileName: String,
    val type: TransferType,
    val state: TransferState,
    val size: String,
    val handle: Long,
    var path: String,
    val displayPath: String?,
    var isOffline: Boolean?,
    val timestamp: Long,
    val error: String?,
    val errorCode: Int?,
    val originalPath: String,
    val parentHandle: Long,
    override val appData: List<TransferAppData>,
    override val uniqueId: Long = 0L,
    override val totalBytes: Long = 0L,
) : ActiveTransfer {
    override val isFinished = true
    override val isFolderTransfer = false
    override val isAlreadyTransferred = false
    override val isPaused = false
    override val localPath = originalPath
    override val tag = 0
    override val transferType = type

    /**
     * Whether the transfer is a download to the local storage and the download location is a content URI.
     */
    val isContentUriDownload
        get() = isOffline == false
                && type == TransferType.DOWNLOAD // Assuming 0 represents MegaTransfer.TYPE_DOWNLOAD
                && path.startsWith(File.separator).not()

    /**
     * Whether the transfer finished with an error.
     */
    val isError
        get() = state == TransferState.STATE_FAILED

    /**
     * Whether the transfer was cancelled.
     */
    override val isCancelled
        get() = state == TransferState.STATE_CANCELLED
}