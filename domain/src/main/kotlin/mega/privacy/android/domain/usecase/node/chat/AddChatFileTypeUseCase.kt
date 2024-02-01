package mega.privacy.android.domain.usecase.node.chat

import mega.privacy.android.domain.entity.node.DefaultTypedFileNode
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.chat.ChatDefaultFile
import mega.privacy.android.domain.entity.node.chat.ChatFile
import mega.privacy.android.domain.entity.node.chat.ChatImageFile
import mega.privacy.android.domain.usecase.node.AddImageTypeUseCase
import javax.inject.Inject

/**
 * Adds chat type to an untyped file node
 */
class AddChatFileTypeUseCase @Inject constructor(
    private val addImageTypeUseCase: AddImageTypeUseCase,
) {

    /**
     * Invoke
     *
     * @param fileNode The untyped file node to which the chat type will be added.
     * @param chatId The ID of the chat to which the file belongs.
     * @param messageId The ID of the message associated with the file.
     * @param messageIndex The index of the node in the message (default is 0 because for now we only have one node for message).
     * @return A [ChatFile] representing the typed chat file based on the provided file node.
     */
    suspend operator fun invoke(
        fileNode: FileNode,
        chatId: Long,
        messageId: Long,
        messageIndex: Int = 0,
    ): ChatFile =
        when (fileNode) {
            is ImageNode -> {
                ChatImageFile(
                    addImageTypeUseCase(fileNode),
                    chatId,
                    messageId,
                    messageIndex
                )
            }

            else -> ChatDefaultFile(DefaultTypedFileNode(fileNode), chatId, messageId, messageIndex)
        }
}