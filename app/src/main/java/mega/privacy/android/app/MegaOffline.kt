package mega.privacy.android.app

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Data class for a node available offline.
 *
 * @property id             ID of the node available offline
 * @property handle         Handle of the node available offline
 * @property path           Path of the node available offline
 * @property name           Name of the node available offline
 * @property parentId       ID of the parent node of the node available offline
 * @property type           Type of the node available offline (file or folder)
 * @property origin         Origin of the node available offline (Incoming share, Backups or Other)
 * @property handleIncoming Handle of the node available offline in case it comes from an incoming share
 */
@Deprecated(
    message = "MegaOffline has been deprecated in favour of OfflineInformation",
    replaceWith = ReplaceWith(
        expression = "mega.privacy.android.data.model.node.OfflineInformation"
    ),
    level = DeprecationLevel.WARNING
)
@Parcelize
data class MegaOffline(
    var id: Int = -1,
    var handle: String = "",
    var path: String = "",
    var name: String = "",
    var parentId: Int = -1,
    var type: String? = "",
    var origin: Int = OTHER,
    var handleIncoming: String = "",
) : Parcelable {

    constructor(
        handle: String,
        path: String,
        name: String,
        parentId: Int,
        type: String?,
        origin: Int,
        handleIncoming: String,
    ) : this() {
        this.handle = handle
        this.path = path
        this.name = name
        this.parentId = parentId
        this.type = type
        this.origin = origin
        this.handleIncoming = handleIncoming
    }

    /**
     * Returns if the MegaOffline is a folder or not
     */
    val isFolder: Boolean
        get() = type == FOLDER

    companion object {
        /** Value to indicate that the node available offline is a folder */
        const val FOLDER = "1"

        /** Value to indicate that the origin of the node available offline is an incoming share */
        const val INCOMING = 1

        /** Value to indicate that the origin of the node available offline is the Backups */
        const val BACKUPS = 2

        /** Value to indicate that the origin of the node available offline is other different than an incoming share or Backups */
        const val OTHER = 0
    }
}