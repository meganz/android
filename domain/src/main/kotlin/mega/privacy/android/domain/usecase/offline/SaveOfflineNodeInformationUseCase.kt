package mega.privacy.android.domain.usecase.offline

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.offline.OfflineNodeInformation
import mega.privacy.android.domain.repository.NodeRepository

import javax.inject.Inject

/**
 * Save the [OfflineNodeInformation] of this node, also all needed ancestors recursively and all children if it's a folder
 */
class SaveOfflineNodeInformationUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
    private val getOfflineNodeInformationUseCase: GetOfflineNodeInformationUseCase,
) {
    /**
     * invoke the use case
     * @param nodeId [NodeId] of the node
     */
    suspend operator fun invoke(nodeId: NodeId) {
        //we don't want to save backup parent node (Vault)
        val backupRootNodeId =
            runCatching { nodeRepository.getBackupFolderId() }.getOrNull() ?: NodeId(-1L)
        val driveRootNode = nodeRepository.getRootNode()?.id ?: NodeId(-1L)
        nodeRepository.getNodeById(nodeId)?.let { node ->
            //we need to save parents before the node itself
            saveNodeAndItsParentsRecursively(
                nodeId = node.id,
                driveRootNodeId = driveRootNode,
                backupRootNodeId = backupRootNodeId,
            )
            //and then node's children
            if (node is FolderNode) {
                saveChildrenRecursively(node)
            }
        }
    }

    /**
     * Save offline information of all node's parents (not already saved) and then the node itself.
     * As offline information in the database has a reference to it's parent id, we need to save in that way
     * Root drive node and backup root parent are not saved.
     * @param nodeId the [NodeId] of the node we want to save
     * @param driveRootNodeId this node won't be saved as its children appear as root nodes in offline
     * @param backupRootNodeId this node needs to be saved, but not its parent ("Vault")
     */
    private suspend fun saveNodeAndItsParentsRecursively(
        nodeId: NodeId,
        driveRootNodeId: NodeId,
        backupRootNodeId: NodeId,
    ): Boolean {
        if (nodeId != driveRootNodeId && nodeRepository.getOfflineNodeInformation(nodeId) == null) {
            //only save offline information if not already saved and not the drive root node
            nodeRepository.getNodeById(nodeId)?.let { node ->
                //we need to first save all parents recursively except explicitly skipped
                val parentSaved = if (node.id == backupRootNodeId) {
                    false
                } else {
                    saveNodeAndItsParentsRecursively(
                        nodeId = node.parentId,
                        driveRootNodeId = driveRootNodeId,
                        backupRootNodeId = backupRootNodeId,
                    )
                }
                nodeRepository.saveOfflineNodeInformation(
                    getOfflineNodeInformationUseCase(node),
                    if (parentSaved) node.parentId else null
                )
                return true
            }
        }
        return false
    }

    /**
     * Save offline information of all children and sub-children of this folder.
     * When a folder node is saved offline, all its children needs to be saved.
     */
    private suspend fun saveChildrenRecursively(folderNode: FolderNode) {
        folderNode.fetchChildren(SortOrder.ORDER_NONE).filter { node ->
            //no need to save empty folders.
            !(node is FolderNode && node.childFileCount == 0 && node.childFolderCount == 0)
        }.forEach { node ->
            if (nodeRepository.getOfflineNodeInformation(node.id) == null) {
                nodeRepository.saveOfflineNodeInformation(
                    getOfflineNodeInformationUseCase(node),
                    folderNode.id
                )
            }
            if (node is FolderNode) {
                saveChildrenRecursively(node)
            }
        }
    }
}