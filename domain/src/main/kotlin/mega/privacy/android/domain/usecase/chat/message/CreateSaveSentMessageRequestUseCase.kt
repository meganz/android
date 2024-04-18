package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.messages.request.CreateTypedMessageRequest
import javax.inject.Inject

/**
 * Use case to create a request to save a sent message
 */
class CreateSaveSentMessageRequestUseCase @Inject constructor() {

    /**
     * Invoke
     *
     * @param chatMessage
     */
    operator fun invoke(
        chatMessage: ChatMessage,
        chatId: Long,
    ) = CreateTypedMessageRequest(
        chatMessage = chatMessage.copy(messageId = chatMessage.tempId),
        chatId = chatId,
        isMine = true,
        reactions = emptyList(),
        exists = true,
    )
}