package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use case for sending a location message to a chat.
 */
class SendLocationMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val createSaveSentMessageRequestUseCase: CreateSaveSentMessageRequestUseCase,
) {

    /**
     * Invoke.
     *
     * @param chatId Chat id.
     * @param longitude Location longitude.
     * @param latitude Location latitude.
     * @param image Image to be sent.
     */
    suspend operator fun invoke(
        chatId: Long,
        longitude: Float,
        latitude: Float,
        image: String,
    ) {
        val sentMessage = chatRepository.sendGeolocation(chatId, longitude, latitude, image)
        val request = createSaveSentMessageRequestUseCase(sentMessage)
        chatRepository.storeMessages(chatId, listOf(request))
    }
}