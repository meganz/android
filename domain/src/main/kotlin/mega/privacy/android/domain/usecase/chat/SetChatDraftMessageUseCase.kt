package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Set Chat Room Preference Use Case
 *
 */
class SetChatDraftMessageUseCase @Inject constructor(
    private val repository: ChatRepository,
) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke(chatId: Long, draftMessage: String) =
        repository.setChatDraftMessage(chatId, draftMessage)
}