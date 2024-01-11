package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.message.request.CreateTypedMessageRequest
import mega.privacy.android.domain.entity.chat.messages.normal.NormalMessage
import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use case for sending a text message to a chat.
 */
class SendTextMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val createNormalChatMessageUseCase: CreateNormalChatMessageUseCase,
) {

    /**
     * Invoke.
     *
     * @param chatId Chat id.
     * @param message Message to send.
     * @return Temporal [] for showing in UI.
     */
    suspend operator fun invoke(chatId: Long, message: String): NormalMessage {
        val request = CreateTypedMessageRequest(
            chatRepository.sendMessage(chatId, message),
            true,
            false,
            false,
            false
        )
        return createNormalChatMessageUseCase(request)
    }
}