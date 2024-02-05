package mega.privacy.android.domain.usecase.chat.message.reactions

import mega.privacy.android.domain.entity.chat.messages.reactions.Reaction
import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use case for getting reactions of a chat message.
 */
class GetMessageReactionsUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {

    /**
     * Invoke.
     *
     * @param chatId Chat ID.
     * @param msgId Message ID.
     * @return List of [Reaction].
     */
    suspend operator fun invoke(chatId: Long, msgId: Long, myUserHandle: Long) = buildList {
        chatRepository.getMessageReactions(chatId, msgId).forEach { reaction ->
            val count = chatRepository.getMessageReactionCount(chatId, msgId, reaction)
            val userHandles = chatRepository.getReactionUsers(chatId, msgId, reaction)
            val hasMe = myUserHandle in userHandles
            add(Reaction(reaction, count, userHandles, hasMe))
        }
    }
}