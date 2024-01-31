package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use case for sending a text message to a chat.
 */
class SendTextMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val createSaveSentMessageRequestUseCase: CreateSaveSentMessageRequestUseCase,
) {

    /**
     * Invoke.
     *
     * @param chatId Chat id.
     * @param message Message to send.
     */
    suspend operator fun invoke(chatId: Long, message: String) {
        val sentMessage = chatRepository.sendMessage(chatId, message)
        val request = createSaveSentMessageRequestUseCase(sentMessage)
        chatRepository.storeMessages(chatId, listOf(request))
    }
}