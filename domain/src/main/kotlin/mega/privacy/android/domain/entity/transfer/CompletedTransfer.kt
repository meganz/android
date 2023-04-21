package mega.privacy.android.domain.entity.transfer

/**
 * A representation of a completed transfer
 *
 * @property id to uniquely identify the transfer in local storage
 * @property fileName the name of the file
 * @property type the transfer type (eg. TYPE_DOWNLOAD, TYPE_UPLOAD)
 * @property state the transfer state (eg. STATE_COMPLETED, STATE_CANCELLED, STATE_FAILED)
 * @property size the size of the file
 * @property nodeHandle the handle of the node associated to the transfer
 * @property path the path of the node in case of a transfer to the cloud, the destination path in case of a transfer to SD card
 * @property isOfflineFile true if the file is available offline
 * @property error the int representing the errorCode returned by API
 * @property originalPath the original local path of the file transferred
 * @property parentHandle  the parent handle of the file transferred
 * @property timeStamp represents the time when the transfer is completed
 */
data class CompletedTransfer(
    var id: Long = 0,
    val fileName: String?,
    val type: Int,
    val state: Int,
    val size: String?,
    val nodeHandle: String?,
    var path: String?,
    var isOfflineFile: Boolean = false,
    val timeStamp: Long,
    val error: String?,
    val originalPath: String?,
    val parentHandle: Long,
)
