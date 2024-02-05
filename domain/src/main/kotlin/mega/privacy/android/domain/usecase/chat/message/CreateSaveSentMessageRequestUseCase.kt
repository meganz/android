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
    ) = CreateTypedMessageRequest(
        chatMessage = chatMessage.copy(msgId = chatMessage.tempId),
        isMine = true,
        shouldShowAvatar = false,
        shouldShowTime = false,
        shouldShowDate = false,
        reactions = emptyList(),
    )
}