package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Clear chat history use case
 *
 * @property chatRepository
 */
class ClearChatHistoryUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {

    /**
     * clear the entire history of a chat
     *
     * @param chatId
     */
    suspend operator fun invoke(chatId: Long) =
        chatRepository.clearChatHistory(chatId)
}
