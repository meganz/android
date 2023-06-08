package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NameCollisionType
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollision
import mega.privacy.android.domain.entity.node.NodeNameCollisionResult
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.exception.node.NodeDoesNotExistsException
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.IsNodeInRubbish
import javax.inject.Inject

/**
 * Check nodes name collision use case
 *
 * @property isNodeInRubbish
 * @property getChildNodeUseCase
 * @property getNodeByHandlesUseCase
 * @property getRootNodeUseCase
 * @property nodeRepository
 */
class CheckNodesNameCollisionUseCase @Inject constructor(
    private val isNodeInRubbish: IsNodeInRubbish,
    private val getChildNodeUseCase: GetChildNodeUseCase,
    private val getNodeByHandlesUseCase: GetNodeByHandlesUseCase,
    private val getRootNodeUseCase: GetRootNodeUseCase,
    private val nodeRepository: NodeRepository,
) {
    suspend operator fun invoke(
        nodes: Map<Long, Long>,
        type: NameCollisionType,
    ): NodeNameCollisionResult {
        val noConflictNodes = hashMapOf<Long, Long>()
        val conflictNodes = hashMapOf<Long, NodeNameCollision>()
        nodes.forEach { entry ->
            val (nodeHandle, parentNodeHandle) = entry
            val parent = getParentOrRootNode(parentNodeHandle)
            if (parent == null || parent is FileNode) {
                noConflictNodes[nodeHandle] = parentNodeHandle
            } else {
                val currentNode =
                    getNodeByHandlesUseCase(nodeHandle) ?: throw NodeDoesNotExistsException()
                getChildNodeUseCase(
                    NodeId(parentNodeHandle),
                    currentNode.name
                )?.let { conflictNode ->
                    conflictNodes[nodeHandle] =
                        createNodeNameCollision(currentNode, parent, conflictNode)
                } ?: run {
                    noConflictNodes[nodeHandle] = parentNodeHandle
                }
            }
        }

        return NodeNameCollisionResult(noConflictNodes, conflictNodes, type)
    }

    private fun createNodeNameCollision(
        currentNode: UnTypedNode,
        parent: UnTypedNode,
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
        isFile = currentNode is FileNode
    )

    private suspend fun getParentOrRootNode(parentHandle: Long) =
        if (parentHandle == nodeRepository.getInvalidHandle()) {
            getRootNodeUseCase()
        } else {
            getNodeByHandlesUseCase(parentHandle)
        }?.takeUnless { isNodeInRubbish(parentHandle) }
}