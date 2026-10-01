package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Close Chat Preview Use Case
 *
 */
class CloseChatPreviewUseCase @Inject constructor(
    private val repository: ChatRepository,
) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke(chatId: Long) = repository.closeChatPreview(chatId)
}