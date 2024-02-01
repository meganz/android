package mega.privacy.android.domain.usecase.node.chat

import mega.privacy.android.domain.entity.node.chat.ChatFile
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Get the chat file corresponding to this chat and message if exists, null otherwise
 *
 */
class GetChatFileUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
    private val addChatFileTypeUseCase: AddChatFileTypeUseCase,
) {

    /**
     *  Get the chat file corresponding to this chat and message if exists, null otherwise
     *
     *  @param chatId
     *  @param messageId
     *  @param messageIndex: The index of the file in message attachments, usually 0 since there's usually only one file. Keeping this because the SDK, in theory, allows for multiple files per message.
     */
    suspend operator fun invoke(chatId: Long, messageId: Long, messageIndex: Int = 0): ChatFile? =
        when (
            val fileNode = nodeRepository.getNodeFromChatMessage(chatId, messageId, messageIndex)
        ) {
            null -> null
            else -> addChatFileTypeUseCase(fileNode, chatId, messageId, messageIndex)
        }
}