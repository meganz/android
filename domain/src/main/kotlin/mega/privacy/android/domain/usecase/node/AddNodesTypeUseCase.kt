package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.FolderType
import mega.privacy.android.domain.entity.node.DefaultTypedFileNode
import mega.privacy.android.domain.entity.node.DefaultTypedFolderNode
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.usecase.GetDeviceType
import mega.privacy.android.domain.usecase.HasAncestor
import javax.inject.Inject

/**
 * Add nodes type use case
 *
 */
class AddNodesTypeUseCase @Inject constructor(
    private val getGroupFolderTypeUseCase: GetGroupFolderTypeUseCase,
    private val getDeviceType: GetDeviceType,
    private val hasAncestor: HasAncestor,
) {
    /**
     * Invoke
     *
     * @param nodes
     */
    suspend operator fun invoke(nodes: List<UnTypedNode>): List<TypedNode> {
        val groupFolderTypes = getGroupFolderTypeUseCase()
        val parentBackups = mutableMapOf<NodeId, Boolean>()
        val backupNodeId = groupFolderTypes.entries.find { it.value == FolderType.RootBackup }?.key
        return nodes.map { node ->
            when (node) {
                is TypedNode -> node
                is FileNode -> DefaultTypedFileNode(node)
                is FolderNode -> {
                    // if nodes are in same parent, we can reuse the parent backup status
                    // hasAncestor is expensive, it's recursive execution
                    val isParentInBackUp = parentBackups.getOrPut(node.parentId) {
                        backupNodeId?.let {
                            hasAncestor(
                                targetNodeId = node.parentId,
                                ancestorId = it
                            )
                        } ?: false
                    }
                    DefaultTypedFolderNode(
                        folderNode = node,
                        type = getFolderType(node, groupFolderTypes, isParentInBackUp)
                    )
                }
            }
        }
    }

    private suspend fun getFolderType(
        node: FolderNode,
        groupFolderTypes: Map<NodeId, FolderType>,
        isParentInBackUp: Boolean,
    ): FolderType {
        return when {
            groupFolderTypes[node.id] != null -> groupFolderTypes[node.id]!!
            !node.device.isNullOrEmpty() -> FolderType.DeviceBackup(getDeviceType(node))
            isParentInBackUp -> FolderType.ChildBackup
            else -> FolderType.Default
        }
    }
}