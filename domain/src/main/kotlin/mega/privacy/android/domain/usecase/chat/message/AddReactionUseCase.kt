package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use case for adding a reaction to a chat message.
 */
class AddReactionUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {
    /**
     * Invoke.
     *
     * @param chatId Chat ID.
     * @param msgId Message ID.
     * @param reaction Reaction to add.
     */
    suspend operator fun invoke(chatId: Long, msgId: Long, reaction: String) =
        chatRepository.addReaction(chatId, msgId, reaction)
}