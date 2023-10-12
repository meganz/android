package mega.privacy.android.domain.entity


/**
 * Offline information
 *
 * @property id
 * @property handle
 * @property path
 * @property name
 * @property parentId
 * @property type
 * @property origin
 * @property handleIncoming
 */
data class Offline(
    val id: Int,
    val handle: String,
    val path: String,
    val name: String,
    val parentId: Int,
    val type: String?,
    val origin: Int,
    val handleIncoming: String,
) {

    /**
     * Returns if the MegaOffline is a folder or not
     */
    val isFolder: Boolean
        get() = type == FOLDER

    companion object {
        /** Value to indicate that the node available offline is a folder */
        const val FOLDER = "1"

        /** Value to indicate that the node available offline is a file */
        const val FILE = "0"

        /** Value to indicate that the origin of the node available offline is an incoming share */
        const val INCOMING = 1

        /** Value to indicate that the origin of the node available offline is the Backups */
        const val BACKUPS = 2

        /** Value to indicate that the origin of the node available offline is other different than an incoming share or Backups */
        const val OTHER = 0
    }
}
