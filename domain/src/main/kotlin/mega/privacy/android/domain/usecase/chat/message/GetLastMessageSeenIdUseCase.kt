package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import javax.inject.Inject

/**
 * Get Last Message Seen Id Use Case
 *
 */
class GetLastMessageSeenIdUseCase @Inject constructor(
    private val repository: ChatMessageRepository,
) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke(
        chatId: Long,
    ) = repository.getLastMessageSeenId(chatId)
}