package mega.privacy.android.domain.entity.offline

/**
 * OfflineFileInformation
 *
 * @property id id of node in database
 * @property handle handle of node
 * @property parentId id of parent in database
 * @property name node's name
 * @property totalSize Size of the node, for folder it will be the total size of its content
 * @property isFolder true if the node is a folder, false if it's a file
 * @property folderInfo number of files in a folder
 * @property addedTime creation time of the node
 * @property thumbnail thumbnail of the node
 */
data class OfflineFileInformation(
    val id: Int = 0,
    val handle: Long = 0,
    val parentId: Int = -1,
    val name: String = "",
    val totalSize: Long = 0L,
    val isFolder: Boolean = false,
    val folderInfo: OfflineFolderInfo? = null,
    val addedTime: Long? = null,
    val thumbnail: String? = null,
)