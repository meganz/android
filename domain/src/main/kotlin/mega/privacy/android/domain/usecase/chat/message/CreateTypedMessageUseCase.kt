package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.message.request.CreateTypedMessageInfo
import mega.privacy.android.domain.entity.chat.messages.TypedMessage

/**
 * Create typed message use case
 */
interface CreateTypedMessageUseCase {

    /**
     * Invoke
     *
     * @param request [CreateTypedMessageInfo]
     * @return [TypedMessage]
     */
    operator fun invoke(request: CreateTypedMessageInfo): TypedMessage
}

