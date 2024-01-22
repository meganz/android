package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.chat.messages.request.CreateTypedMessageInfo

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

