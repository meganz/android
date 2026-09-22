package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollision
import mega.privacy.android.domain.exception.extension.shouldEmitErrorForNodeMovement
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.node.chat.GetChatFilesUseCase
import javax.inject.Inject

/**
 * Use Case to copy collided node and move it to a new Node while updating its name
 */
class CopyCollidedNodeUseCase @Inject constructor(
    private val getChatFilesUseCase: GetChatFilesUseCase,
    private val copyTypedNodeUseCase: CopyTypedNodeUseCase,
    private val nodeRepository: NodeRepository,
) {

    /**
     * Invoke
     *
     * @param nameCollision Node name collision
     * @param rename True if the node should be renamed, false otherwise
     *
     */
    suspend operator fun invoke(
        nameCollision: NodeNameCollision,
        rename: Boolean,
    ): MoveRequestResult {
        val newNodeParent = NodeId(nameCollision.parentHandle)
        val newNodeName = if (rename) nameCollision.renameName else null
        try {
            when (nameCollision) {
                is NodeNameCollision.Chat -> {
                    val nodes = getChatFilesUseCase(
                        nameCollision.chatId,
                        nameCollision.messageId
                    )
                    val chatNode = nodes.firstOrNull { it.id.longValue == nameCollision.nodeHandle }

                    requireNotNull(chatNode) { "Chat file not found" }
                    copyTypedNodeUseCase(
                        nodeToCopy = chatNode,
                        newNodeParent = newNodeParent,
                        newNodeName = newNodeName
                    )
                }

                is NodeNameCollision.FileLink -> {
                    val publicNode =
                        nodeRepository.getNodeFromSerializedData(nameCollision.serializedData)
                    requireNotNull(publicNode) { "Public node could not be deserialized" }
                    nodeRepository.copyPublicNode(
                        publicNodeToCopy = publicNode,
                        newNodeParent = newNodeParent,
                        newNodeName = newNodeName
                    )
                }

                is NodeNameCollision.Default -> nodeRepository.copyNode(
                    nodeToCopy = NodeId(nameCollision.nodeHandle),
                    newNodeParent = newNodeParent,
                    newNodeName = newNodeName
                )
            }
            return MoveRequestResult.Copy(
                count = 1,
                errorCount = 0,
            )
        } catch (it: Throwable) {
            if (it.shouldEmitErrorForNodeMovement()) throw it
            return MoveRequestResult.Copy(
                count = 1,
                errorCount = 1,
            )
        }
    }

}