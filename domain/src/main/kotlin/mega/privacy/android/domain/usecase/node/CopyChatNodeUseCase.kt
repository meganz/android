package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.node.chat.GetChatFileUseCase
import javax.inject.Inject

/**
 * Use Case to copy a chat Node and move it to a new Node while updating its name
 */
class CopyChatNodeUseCase @Inject constructor(
    private val getChatFileUseCase: GetChatFileUseCase,
    private val copyTypedNodeUseCase: CopyTypedNodeUseCase,
) {

    /**
     * Copy a node to another node while updating its name
     * @param chatId The ID of the chat
     * @param messageId The ID of the message
     * @param messageIndex: The index of the file in message attachments, usually 0 since there's usually only one file.
     * @param newNodeParent the Node when the chat node will be moved to
     * @param newNodeName the new name for chat node once it is moved to [newNodeParent]
     *
     * @return the node id of the new Node that was copied
     *
     * @throws IllegalStateException if the chat file is not found
     */
    suspend operator fun invoke(
        chatId: Long,
        messageId: Long,
        messageIndex: Int = 0,
        newNodeName: String? = null,
        newNodeParent: NodeId,
    ) = getChatFileUseCase(chatId, messageId, messageIndex)?.let {
        copyTypedNodeUseCase(it, newNodeParent, newNodeName)
    } ?: throw IllegalStateException("Chat file not found")

}
