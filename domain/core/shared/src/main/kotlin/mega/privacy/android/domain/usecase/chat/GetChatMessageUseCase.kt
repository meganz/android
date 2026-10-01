package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use case for getting a chat message.
 */
class GetChatMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {

    /**
     * Invoke.
     *
     * @param chatId MegaChatHandle that identifies the chat room
     * @param msgId MegaChatHandle that identifies the message
     * @return The [ChatMessage], or NULL if not found.
     */
    suspend operator fun invoke(chatId: Long, msgId: Long) =
        chatRepository.getMessage(chatId, msgId)
            ?: chatRepository.getMessageFromNodeHistory(chatId, msgId)
}