package mega.privacy.android.domain.usecase.chat.message.reactions

import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import javax.inject.Inject

/**
 * Use case for adding a reaction to a chat message.
 */
class AddReactionUseCase @Inject constructor(
    private val chatMessageRepository: ChatMessageRepository,
) {
    /**
     * Invoke.
     *
     * @param chatId Chat ID.
     * @param msgId Message ID.
     * @param reaction Reaction to add.
     */
    suspend operator fun invoke(chatId: Long, msgId: Long, reaction: String) =
        chatMessageRepository.addReaction(chatId, msgId, reaction)
}