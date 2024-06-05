package mega.privacy.android.data.mapper.zipbrowser

import mega.privacy.android.domain.entity.zipbrowser.ZipEntryType
import mega.privacy.android.domain.entity.zipbrowser.ZipTreeNode
import java.util.zip.ZipEntry
import javax.inject.Inject

/**
 * The mapper class to convert the ZipEntry to ZipTreeNode
 */
class ZipTreeNodeMapper @Inject constructor() {

    /**
     * Convert the ZipEntry to ZipTreeNode
     *
     * @param zipEntry ZipEntry
     * @param name zip entry name
     * @param path zip entry path
     * @param parentPath the parent path of current zip entry
     * @return ZipTreeNode
     */
    operator fun invoke(
        zipEntry: ZipEntry,
        name: String,
        path: String,
        parentPath: String?,
        zipEntryType: ZipEntryType,
    ) = ZipTreeNode(
        name = name,
        path = path,
        size = zipEntry.size,
        zipEntryType = zipEntryType,
        parentPath = parentPath,
        children = emptyList()
    )
}