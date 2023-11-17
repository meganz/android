package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * End Call Use Case
 *
 */
class EndCallUseCase @Inject constructor(
    private val repository: ChatRepository,
) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke(chatId: Long) =
        repository.endChatCall(chatId)
}