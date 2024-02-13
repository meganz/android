package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import mega.privacy.android.domain.usecase.chat.GetChatMessageUseCase
import javax.inject.Inject

/**
 * Attach node use case
 *
 * @property chatRepository [ChatRepository]
 */
class AttachNodeUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val chatMessageRepository: ChatMessageRepository,
    private val createSaveSentMessageRequestUseCase: CreateSaveSentMessageRequestUseCase,
    private val getChatMessageUseCase: GetChatMessageUseCase,
) {

    /**
     * Invoke
     *
     * @param chatId Chat identifier.
     * @param fileNode [FileNode].
     */
    suspend operator fun invoke(chatId: Long, fileNode: FileNode) {
        // TODO: This use case is only valid for nodes in the current logged in account.
        //  The behaviour for checking if node is mine or not should be implemented.
        //  If the node is not mine, then it should be copied into "My chat files folder" and then attached.
        //  Better to create a new GetMyFileNodeUseCase for this purpose as it will be used in other places
        chatMessageRepository.attachNode(chatId, fileNode.id.longValue)?.let {
            getChatMessageUseCase(chatId, it)?.let { message ->
                val request = createSaveSentMessageRequestUseCase(message, chatId)
                chatRepository.storeMessages(listOf(request))
            }
        }
    }
}