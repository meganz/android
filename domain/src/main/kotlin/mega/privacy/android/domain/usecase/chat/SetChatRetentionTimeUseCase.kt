package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use case to set the chat's retention time
 *
 * @property chatRepository [ChatRepository]
 */
class SetChatRetentionTimeUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {

    /**
     * Invocation method
     *
     * @param chatId Chat room ID
     * @param period Retention timeframe in seconds, after which older messages in the chat are automatically deleted
     */
    suspend operator fun invoke(chatId: Long, period: Long) =
        chatRepository.setChatRetentionTime(chatId, period)
}
