package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.message.request.CreateTypedMessageRequest
import mega.privacy.android.domain.entity.chat.messages.TypedMessage

/**
 * Create typed message use case
 */
interface CreateTypedMessageUseCase {

    /**
     * Invoke
     *
     * @param message [ChatMessage]
     * @param isMine True if the message is mine.
     * @return [TypedMessage]
     */
    operator fun invoke(request: CreateTypedMessageRequest): TypedMessage
}

