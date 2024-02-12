package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import javax.inject.Inject

/**
 * Use case for forwarding one contact to one or more chats.
 */
class ForwardContactUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val chatMessageRepository: ChatMessageRepository,
    private val createSaveSentMessageRequestUseCase: CreateSaveSentMessageRequestUseCase,
) {

    /**
     * Invoke.
     *
     * @param sourceChatId Source chat id.
     * @param msgId Message id.
     * @param targetChatId Chat id where to forward.
     */
    suspend operator fun invoke(sourceChatId: Long, msgId: Long, targetChatId: Long) {
        chatMessageRepository.forwardContact(sourceChatId, msgId, targetChatId)?.let {
            val request = createSaveSentMessageRequestUseCase(it)
            chatRepository.storeMessages(targetChatId, listOf(request))
        }
    }
}