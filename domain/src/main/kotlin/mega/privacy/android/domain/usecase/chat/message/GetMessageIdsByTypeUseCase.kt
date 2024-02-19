package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.ChatMessageType
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import javax.inject.Inject

/**
 * Get Message Ids By Type
 *
 */
class GetMessageIdsByTypeUseCase @Inject constructor(
    private val repository: ChatMessageRepository,
) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke(
        chatId: Long, type: ChatMessageType,
    ) = repository.getMessageIdsByType(chatId, type)
}