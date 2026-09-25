package mega.privacy.android.domain.usecase.chat.message.forward

import mega.privacy.android.domain.entity.chat.messages.ForwardResult
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.chat.messages.meta.RichPreviewMessage
import mega.privacy.android.domain.usecase.chat.message.SendTextMessageUseCase
import javax.inject.Inject

/**
 * Use case to forward a rich preview.
 */
class ForwardRichPreviewUseCase @Inject constructor(
    private val sendTextMessageUseCase: SendTextMessageUseCase,
) : ForwardMessageUseCase() {
    override suspend fun forwardMessage(targetChatId: Long, message: TypedMessage): ForwardResult? {
        val richPreviewMessage = message as? RichPreviewMessage ?: return null
        sendTextMessageUseCase(chatId = targetChatId, message = richPreviewMessage.content)
        return ForwardResult.Success(targetChatId)
    }

}