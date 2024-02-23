package mega.privacy.android.domain.usecase.chat.message.edit

import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.chat.messages.normal.NormalMessage
import javax.inject.Inject

/**
 * Use case for getting content from a normal message.
 */
class GetContentFromNormalMessageUseCase @Inject constructor() : GetContentFromMessageUseCase() {
    override suspend fun getContent(message: TypedMessage) =
        (message as? NormalMessage)?.content
}