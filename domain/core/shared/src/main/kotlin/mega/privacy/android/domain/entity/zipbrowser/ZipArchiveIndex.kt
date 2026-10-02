package mega.privacy.android.domain.entity.zipbrowser

/**
 * The whole folder structure of an archive, built from its central directory. Holds no file handles
 * and no extracted data, so browsing it costs nothing beyond the initial read.
 *
 * @property entries every entry keyed by its path, including the folders that entry paths imply but
 * that the archive never declared as entries of their own
 * @property isEncrypted true when at least one entry is encrypted. Listing an encrypted archive
 * still works, because the central directory is not encrypted.
 */
data class ZipArchiveIndex(
    val archivePath: String,
    val entries: Map<String, ZipArchiveEntry>,
    val isEncrypted: Boolean,
) {
    private val childrenByParent: Map<String?, List<ZipArchiveEntry>> by lazy {
        entries.values.sortedBy { it.index }.groupBy { it.parentPath }
    }

    /**
     * The direct children of a folder, in central directory order.
     *
     * @param parentPath the folder, or null for the archive root
     */
    fun childrenOf(parentPath: String?): List<ZipArchiveEntry> =
        childrenByParent[parentPath].orEmpty()

    fun folderCountOf(parentPath: String?): Int =
        childrenOf(parentPath).count { it.isDirectory }

    /**
     * Recursive file count of a subtree, which is what a folder row reports.
     */
    fun fileCountOf(parentPath: String?): Int =
        childrenOf(parentPath).sumOf { child ->
            if (child.isDirectory) fileCountOf(child.path) else 1
        }
}
