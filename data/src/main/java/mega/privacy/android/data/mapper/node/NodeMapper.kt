package mega.privacy.android.data.mapper.node

import mega.privacy.android.domain.entity.Offline
import mega.privacy.android.domain.entity.node.NodeId
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
     * @param megaNode  Mega node to map
     * @param fromFolderLink    If the node mapping is from folder link
     * @param requireSerializedData To se the serializedData only when required and not always
     * @param offline    Offline information if the node is available offline
     * @param syncedNodeIds   Set of synced node ids if the node is in a synced folder
     *
     * @return UntypedNode
     */
    suspend operator fun invoke(
        megaNode: MegaNode,
        fromFolderLink: Boolean = false,
        requireSerializedData: Boolean = false,
        offline: Offline? = null,
        syncedNodeIds: Set<NodeId>? = null,
    ) = if (megaNode.isFolder) {
        folderNodeMapper(
            megaNode = megaNode,
            fromFolderLink = fromFolderLink,
            requireSerializedData = requireSerializedData,
            isAvailableOffline = offline != null,
            syncedNodeIds = syncedNodeIds
        )
    } else {
        fileNodeMapper(
            megaNode = megaNode,
            requireSerializedData = requireSerializedData,
            offline = offline
        )
    }
}