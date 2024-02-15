package mega.privacy.android.domain.usecase.chat.message.forward

import mega.privacy.android.domain.entity.chat.messages.ForwardResult
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.chat.messages.meta.LocationMessage
import mega.privacy.android.domain.usecase.chat.message.SendLocationMessageUseCase
import javax.inject.Inject

/**
 * Use case to forward a location.
 */
class ForwardLocationUseCase @Inject constructor(
    private val sendLocationMessageUseCase: SendLocationMessageUseCase,
) : ForwardMessageUseCase() {
    override suspend fun forwardMessage(targetChatId: Long, message: TypedMessage): ForwardResult? {
        val locationMessage = message as? LocationMessage ?: return null
        locationMessage.chatGeolocationInfo?.let {
            sendLocationMessageUseCase(
                chatId = targetChatId,
                longitude = it.longitude,
                latitude = it.latitude,
                image = it.image.orEmpty()
            )
        }
        return ForwardResult.Success(targetChatId)
    }
}