package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Set chat draft message use case
 *
 */
class SetChatDraftMessageUseCase @Inject constructor(
    private val repository: ChatRepository,
) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke(chatId: Long, draftMessage: String, editingMessageId: Long?) =
        repository.setChatDraftMessage(chatId, draftMessage, editingMessageId)
}