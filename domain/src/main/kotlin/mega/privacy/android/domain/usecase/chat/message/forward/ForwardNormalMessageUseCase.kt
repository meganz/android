package mega.privacy.android.domain.usecase.chat.message.forward

import mega.privacy.android.domain.entity.chat.messages.ForwardResult
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.chat.messages.normal.NormalMessage
import mega.privacy.android.domain.usecase.chat.message.SendTextMessageUseCase
import javax.inject.Inject

/**
 * Use case to forward a normal message.
 */
class ForwardNormalMessageUseCase @Inject constructor(
    private val sendTextMessageUseCase: SendTextMessageUseCase,
) : ForwardMessageUseCase() {
    override suspend fun forwardMessage(targetChatId: Long, message: TypedMessage): ForwardResult? {
        val normalMessage = message as? NormalMessage ?: return null
        sendTextMessageUseCase(chatId = targetChatId, message = normalMessage.content.orEmpty())
        return ForwardResult.Success(targetChatId)
    }
}