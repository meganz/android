package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.message.request.CreateTypedMessageRequest
import mega.privacy.android.domain.entity.chat.messages.meta.MetaMessage
import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use case for sending a location message to a chat.
 */
class SendLocationMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val createMetaMessageUseCase: CreateMetaMessageUseCase,
) {

    /**
     * Invoke.
     *
     * @param chatId Chat id.
     * @param longitude Location longitude.
     * @param latitude Location latitude.
     * @param image Image to be sent.
     * @return Temporal [] for showing in UI.
     */
    suspend operator fun invoke(
        chatId: Long,
        longitude: Float,
        latitude: Float,
        image: String,
    ): MetaMessage {
        val request = CreateTypedMessageRequest(
            message = chatRepository.sendGeolocation(chatId, longitude, latitude, image),
            isMine = true,
            shouldShowAvatar = false,
            shouldShowTime = false,
            shouldShowDate = false
        )
        return createMetaMessageUseCase(request)
    }
}