package mega.privacy.android.data.mapper.node

import mega.privacy.android.domain.entity.FolderType
import mega.privacy.android.domain.entity.FolderTypeData
import mega.privacy.android.domain.entity.Offline
import mega.privacy.android.domain.entity.node.DefaultTypedFileNode
import mega.privacy.android.domain.entity.node.DefaultTypedFolderNode
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Mapper that converts MegaNode to TypedNode by combining node conversion and folder type determination.
 */
internal class TypedNodeMapper @Inject constructor(
    private val nodeMapper: NodeMapper,
    private val folderTypeMapper: FolderTypeMapper,
) {

    /**
     * Converts a MegaNode to a TypedNode using pre-fetched folder type data.
     *
     * @param megaNode The MegaNode to convert
     * @param folderTypeData Pre-fetched data for folder type determination
     * @param offline Optional offline information
     * @param fromFolderLink Whether the node is from a folder link
     * @param requireSerializedData Whether to include serialized data
     * @return The converted TypedNode
     */
    suspend operator fun invoke(
        megaNode: MegaNode,
        folderTypeData: FolderTypeData?,
        offline: Offline? = null,
        fromFolderLink: Boolean = false,
        requireSerializedData: Boolean = false,
    ): TypedNode {
        val unTypedNode = nodeMapper(
            megaNode = megaNode,
            fromFolderLink = fromFolderLink,
            requireSerializedData = requireSerializedData,
            offline = offline
        )
        return when (unTypedNode) {
            is TypedNode -> unTypedNode
            is FileNode -> DefaultTypedFileNode(fileNode = unTypedNode)
            is FolderNode -> {
                val folderType = folderTypeData?.let {
                    folderTypeMapper(unTypedNode, it)
                } ?: FolderType.Default
                DefaultTypedFolderNode(
                    folderNode = unTypedNode,
                    type = folderType
                )
            }
        }
    }
}
