package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.node.TypedFileNode
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
    private val getAttachableNodeIdUseCase: GetAttachableNodeIdUseCase,
) {

    /**
     * Invoke
     *
     * @param chatId Chat identifier.
     * @param fileNode [TypedFileNode].
     */
    suspend operator fun invoke(chatId: Long, fileNode: TypedFileNode) {
        val attachableNodeId = getAttachableNodeIdUseCase(fileNode)
        chatMessageRepository.attachNode(chatId, attachableNodeId)?.let {
            getChatMessageUseCase(chatId, it)?.let { message ->
                val request = createSaveSentMessageRequestUseCase(message, chatId)
                chatRepository.storeMessages(listOf(request))
            }
        }
    }
}