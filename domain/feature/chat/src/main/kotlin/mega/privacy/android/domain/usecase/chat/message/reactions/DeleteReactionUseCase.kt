package mega.privacy.android.domain.usecase.chat.message.reactions

import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import javax.inject.Inject

/**
 * Use case for deleting a reaction in a chat message.
 */
class DeleteReactionUseCase @Inject constructor(
    private val chatMessageRepository: ChatMessageRepository,
) {
    /**
     * Invoke.
     *
     * @param chatId Chat ID.
     * @param msgId Message ID.
     * @param reaction Reaction to remove.
     */
    suspend operator fun invoke(chatId: Long, msgId: Long, reaction: String) =
        chatMessageRepository.deleteReaction(chatId, msgId, reaction)
}