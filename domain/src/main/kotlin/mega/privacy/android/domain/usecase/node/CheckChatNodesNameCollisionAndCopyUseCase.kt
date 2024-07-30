package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollision
import mega.privacy.android.domain.entity.node.NodeNameCollisionsResult
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.NodeNameCollisionWithActionResult
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.exception.node.NodeDoesNotExistsException
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.node.chat.GetChatFileUseCase
import javax.inject.Inject

/**
 * Use case to check chat nodes name collision
 */
class CheckChatNodesNameCollisionAndCopyUseCase @Inject constructor(
    private val isNodeInRubbishBinUseCase: IsNodeInRubbishBinUseCase,
    private val getChildNodeUseCase: GetChildNodeUseCase,
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase,
    private val getRootNodeUseCase: GetRootNodeUseCase,
    private val getChatFileUseCase: GetChatFileUseCase,
    private val nodeRepository: NodeRepository,
    private val copyTypedNodesUseCase: CopyTypedNodesUseCase,
) {
    /**
     * Invoke
     *
     * @param chatId The ID of the chat
     * @param messageIds The list of message IDs
     * @param newNodeParent the Node where the chat node will be copied to
     */
    suspend operator fun invoke(
        chatId: Long,
        messageIds: List<Long>,
        newNodeParent: NodeId,
    ): NodeNameCollisionWithActionResult {
        val noConflictNodes = hashMapOf<Long, Long>()
        val conflictNodes = hashMapOf<Long, NodeNameCollision>()
        val nodesToCopy = mutableListOf<TypedNode>()

        messageIds.forEach { messageId ->
            val currentNode = getChatFileUseCase(chatId, messageId, 0)
                ?: throw NodeDoesNotExistsException()
            val nodeHandle = currentNode.id.longValue
            val parentNodeHandle = newNodeParent.longValue
            val parent = getParentOrRootNode(parentNodeHandle)
            if (parent == null || parent is FileNode) {
                noConflictNodes[nodeHandle] = parentNodeHandle
            } else {
                getChildNodeUseCase(
                    parentNodeId = newNodeParent,
                    name = currentNode.name
                )?.let { conflictNode ->
                    conflictNodes[nodeHandle] = createChatNodeNameCollision(
                        currentNode = currentNode,
                        parent = parent,
                        conflictNode = conflictNode,
                        chatId = chatId,
                        messageId = messageId
                    )
                } ?: run {
                    noConflictNodes[nodeHandle] = parentNodeHandle
                    nodesToCopy.add(currentNode)
                }
            }
        }

        val collisionResult = NodeNameCollisionsResult(
            noConflictNodes = noConflictNodes,
            conflictNodes = conflictNodes,
            type = NodeNameCollisionType.COPY
        )

        val moveRequestResult = if (nodesToCopy.isNotEmpty()) {
            copyTypedNodesUseCase(
                nodesToCopy = nodesToCopy,
                newNodeParent = newNodeParent
            )
        } else null

        return NodeNameCollisionWithActionResult(
            collisionResult = collisionResult,
            moveRequestResult = moveRequestResult
        )
    }

    private fun createChatNodeNameCollision(
        currentNode: UnTypedNode,
        parent: Node,
        conflictNode: UnTypedNode,
        chatId: Long,
        messageId: Long,
    ) = NodeNameCollision.Chat(
        collisionHandle = conflictNode.id.longValue,
        nodeHandle = currentNode.id.longValue,
        parentHandle = parent.id.longValue,
        name = currentNode.name,
        size = (currentNode as? FileNode)?.size ?: 0,
        childFolderCount = (parent as? FolderNode)?.childFolderCount ?: 0,
        childFileCount = (parent as? FolderNode)?.childFileCount ?: 0,
        lastModified = if (currentNode is FileNode) currentNode.modificationTime else currentNode.creationTime,
        isFile = currentNode is FileNode,
        chatId = chatId,
        messageId = messageId,
    )

    private suspend fun getParentOrRootNode(parentHandle: Long) =
        if (parentHandle == nodeRepository.getInvalidHandle()) {
            getRootNodeUseCase()
        } else {
            getNodeByHandleUseCase(handle = parentHandle, attemptFromFolderApi = false)
        }?.takeUnless { isNodeInRubbishBinUseCase(NodeId(parentHandle)) }
}
