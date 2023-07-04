package mega.privacy.android.data.mapper.node

import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Node mapper
 *
 * @property folderNodeMapper
 * @property fileNodeMapper
 * @constructor Create empty Node mapper
 */
internal class NodeMapper @Inject constructor(
    private val folderNodeMapper: FolderNodeMapper,
    private val fileNodeMapper: FileNodeMapper,
) {
    /**
     * Invoke
     *
     * @param megaNode
     */
    suspend operator fun invoke(
        megaNode: MegaNode,
        fromFolderLink: Boolean = false,
    ) = if (megaNode.isFolder) {
        folderNodeMapper(megaNode, fromFolderLink)
    } else {
        fileNodeMapper(megaNode)
    }
}