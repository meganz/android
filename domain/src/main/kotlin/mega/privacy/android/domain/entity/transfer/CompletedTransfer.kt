package mega.privacy.android.domain.entity.transfer

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
 * @property isOffline true if the file is available offline
 * @property error the int representing the errorCode returned by API
 * @property originalPath the original local path of the file transferred
 * @property parentHandle  the parent handle of the file transferred
 * @property timestamp represents the time when the transfer is completed
 */
data class CompletedTransfer(
    val id: Int? = null,
    val fileName: String,
    val type: Int,
    val state: Int,
    val size: String,
    val handle: Long,
    var path: String,
    var isOffline: Boolean?,
    val timestamp: Long,
    val error: String?,
    val originalPath: String,
    val parentHandle: Long,
)
