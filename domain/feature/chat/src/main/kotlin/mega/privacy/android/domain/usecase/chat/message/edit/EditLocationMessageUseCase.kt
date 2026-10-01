package mega.privacy.android.domain.usecase.chat.message.edit

import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import javax.inject.Inject

/**
 * Use case for editing a location message.
 */
class EditLocationMessageUseCase @Inject constructor(
    private val chatMessageRepository: ChatMessageRepository,
) {

    /**
     * Invoke.
     *
     * @param chatId Chat ID.
     * @param msgId Message ID.
     * @param longitude New longitude of the message.
     * @param latitude New latitude of the message.
     * @param image New image of the message.
     */
    suspend operator fun invoke(
        chatId: Long,
        msgId: Long,
        longitude: Float,
        latitude: Float,
        image: String,
    ) = chatMessageRepository.editGeolocation(chatId, msgId, longitude, latitude, image)
}