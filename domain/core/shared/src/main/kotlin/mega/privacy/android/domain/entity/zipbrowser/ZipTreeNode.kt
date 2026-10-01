package mega.privacy.android.domain.entity.zipbrowser

/**
 * ZipTreeNode for saving the every node information from zip entries.
 *
 * ZipTreeMap saved all node information, key is zip entry name (zip entry name is unique in zip
 * file).Some zip file doesn't include completed directory structure caused that maybe miss a
 * certain directory by using zip entries to preview zip file. Using ZipTreeMap could create
 * complete directory structure.
 *
 * @property name name for display
 * @property path current zip entry name
 * @property parentPath parent folder path
 * @property size current file size
 * @property zipEntryType ZipEntryType
 * @property children all files or folders under this zip tree node.
 */
data class ZipTreeNode(
    val name: String,
    val path: String,
    val parentPath: String?,
    val size: Long,
    val zipEntryType: ZipEntryType,
    val children: List<ZipTreeNode>
)
