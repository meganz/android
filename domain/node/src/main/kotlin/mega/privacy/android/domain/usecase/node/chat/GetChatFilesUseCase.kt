package mega.privacy.android.domain.usecase.node.chat

import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Get all files corresponding to this chat and message
 *
 */
class GetChatFilesUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
    private val addChatFileTypeUseCase: AddChatFileTypeUseCase,
) {

    /**
     *  Invoke
     *
     *  @param chatId
     *  @param messageId
     */
    suspend operator fun invoke(chatId: Long, messageId: Long) =
        nodeRepository.getNodesFromChatMessage(chatId, messageId)
            .mapIndexed { messageIndex, fileNode ->
                addChatFileTypeUseCase(fileNode, chatId, messageId, messageIndex)
            }
}