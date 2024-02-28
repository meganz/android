package mega.privacy.android.domain.usecase.chat.message.edit

import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.chat.messages.meta.LocationMessage
import javax.inject.Inject

/**
 * Get content(or textMessage) from a [LocationMessage]
 */
class GetContentFromLocationMessageUseCase @Inject constructor() : GetContentFromMessageUseCase() {
    override suspend fun getContent(message: TypedMessage) =
        (message as? LocationMessage)?.content
}
