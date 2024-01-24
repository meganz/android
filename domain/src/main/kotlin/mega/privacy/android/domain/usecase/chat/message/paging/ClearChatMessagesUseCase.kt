package mega.privacy.android.domain.usecase.chat.message.paging

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Clear chat messages use case
 *
 * @property chatRepository
 */
class ClearChatMessagesUseCase @Inject constructor(private val chatRepository: ChatRepository){
    /**
     * Invoke
     *
     * @param chatId
     */
    suspend operator fun invoke(chatId: Long) = chatRepository.clearChatMessages(chatId)
}