package mega.privacy.android.domain.entity.offline

import java.util.concurrent.TimeUnit

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
    val totalSize: Long = 0L,
    val folderInfo: OfflineFolderInfo? = null,
    val thumbnail: String? = null,
    override val id: Int = 0,
    override val handle: String,
    override val parentId: Int = -1,
    override val name: String = "",
    override val isFolder: Boolean = false,
    override val path: String,
    override val lastModifiedTime: Long?,
) : OfflineNodeInformation {
    val addedTime = lastModifiedTime?.let { TimeUnit.MICROSECONDS.toMillis(it) }
}