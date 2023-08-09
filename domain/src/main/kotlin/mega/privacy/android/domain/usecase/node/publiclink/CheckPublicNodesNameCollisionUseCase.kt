package mega.privacy.android.domain.usecase.node.publiclink

import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollision
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.entity.node.publiclink.PublicNodeNameCollisionResult
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.IsNodeInRubbish
import mega.privacy.android.domain.usecase.node.GetChildNodeUseCase
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import javax.inject.Inject

/**
 * Check public link nodes name collision use case
 *
 * @property isNodeInRubbish
 * @property getChildNodeUseCase
 * @property getNodeByHandleUseCase
 * @property getRootNodeUseCase
 * @property nodeRepository
 */
class CheckPublicNodesNameCollisionUseCase @Inject constructor(
    private val isNodeInRubbish: IsNodeInRubbish,
    private val getChildNodeUseCase: GetChildNodeUseCase,
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase,
    private val getRootNodeUseCase: GetRootNodeUseCase,
    private val nodeRepository: NodeRepository,
) {
    /**
     * Invoke
     *
     * @param nodes List of nodes to copy
     * @param targetHandle  Handle of the destination node
     * @param type  Collision type
     */
    suspend operator fun invoke(
        nodes: List<UnTypedNode>,
        targetHandle: Long,
        type: NodeNameCollisionType,
    ): PublicNodeNameCollisionResult {
        val noConflictNodes = mutableListOf<Node>()
        val conflictNodes = mutableListOf<NodeNameCollision>()

        val parent = getParentOrRootNode(targetHandle)
        if (parent == null || parent is FileNode) {
            noConflictNodes.addAll(nodes)
        } else {
            nodes.forEach { currentNode ->
                getChildNodeUseCase(NodeId(targetHandle), currentNode.name)?.let { conflictNode ->
                    conflictNodes.add(createNodeNameCollision(currentNode, parent, conflictNode))
                } ?: run {
                    noConflictNodes.add(currentNode)
                }
            }
        }
        return PublicNodeNameCollisionResult(noConflictNodes, conflictNodes, type)
    }

    private fun createNodeNameCollision(
        currentNode: UnTypedNode,
        parent: Node,
        conflictNode: UnTypedNode,
    ) = NodeNameCollision(
        collisionHandle = conflictNode.id.longValue,
        nodeHandle = currentNode.id.longValue,
        parentHandle = parent.id.longValue,
        name = currentNode.name,
        size = (currentNode as? FileNode)?.size ?: 0,
        childFolderCount = (parent as? FolderNode)?.childFolderCount ?: 0,
        childFileCount = (parent as? FolderNode)?.childFileCount ?: 0,
        lastModified = if (currentNode is FileNode) currentNode.modificationTime else currentNode.creationTime,
        isFile = currentNode is FileNode,
        serializedData = currentNode.serializedData
    )

    private suspend fun getParentOrRootNode(parentHandle: Long) =
        if (parentHandle == nodeRepository.getInvalidHandle()) {
            getRootNodeUseCase()
        } else {
            getNodeByHandleUseCase(parentHandle)
        }?.takeUnless { isNodeInRubbish(parentHandle) }
}