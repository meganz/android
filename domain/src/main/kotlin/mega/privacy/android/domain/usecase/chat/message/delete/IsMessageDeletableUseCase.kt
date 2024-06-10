package mega.privacy.android.domain.usecase.chat.message.delete

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use case for checking if a chat message is deletable.
 */
class IsMessageDeletableUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {

    /**
     * Invoke.
     */
    suspend operator fun invoke(chatId: Long, messageId: Long) =
        chatRepository.getMessage(chatId, messageId)?.isDeletable ?: false
}