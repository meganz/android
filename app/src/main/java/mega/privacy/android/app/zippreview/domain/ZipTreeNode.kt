package mega.privacy.android.app.zippreview.domain

typealias ZipTreeMap = HashMap<String, ZipTreeNode>

/**
 * ZipTreeNode for saving the every node information from zip entries.
 *
 * ZipTreeMap saved all node information, key is zip entry name (zip entry name is unique in zip
 * file).Some zip file doesn't include completed directory structure caused that maybe miss a
 * certain directory by using zip entries to preview zip file. Using ZipTreeMap could create
 * complete directory structure.
 *
 * @param name name for display
 * @param path current zip entry name
 * @param size current file size
 * @param fileType file type
 * @param parent parent for back preview directory
 * @param children all files or folders under this zip tree node.
 */
data class ZipTreeNode(
    val name: String,
    val path: String,
    val size: Long,
    val fileType: FileType,
    val parent: String?,
    val children: MutableList<ZipTreeNode>
)

/**
 * Get zip tree node by path
 * @param path path
 * @return ZipTreeNode
 */
fun ZipTreeMap.getNodeByPath(path: String): ZipTreeNode? {
    return this[path]
}

/**
 * File type
 */
enum class FileType {
    FOLDER, ZIP, FILE
}

/**
 * Get zip tree node name for init zip tree map
 * @return zip tree name
 */
fun String.getZipTreeNodeName() = removeSuffix("/").split("/").last()

/**
 * Get sub path for init zip tree map
 * @param depth depth of sub path
 * @return sub path
 */
fun String.getSubPathByDepth(depth: Int) =
    removeSuffix("/").split("/").take(depth).joinToString("/")

/**
 * Get current zip tree node depth for init zip tree map
 * @return zip tree node depth
 */
fun String.getZipTreeNodeDepth() = removeSuffix("/").split("/").size