package mega.privacy.android.domain.usecase.chat.message.reactions

import mega.privacy.android.domain.entity.chat.messages.reactions.Reaction
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import javax.inject.Inject

/**
 * Use case for getting reactions of a chat message.
 */
class GetReactionsUseCase @Inject constructor(
    private val chatMessageRepository: ChatMessageRepository,
) {

    /**
     * Invoke.
     *
     * @param chatId Chat ID.
     * @param msgId Message ID.
     * @return List of [Reaction].
     */
    suspend operator fun invoke(chatId: Long, msgId: Long, myUserHandle: Long) = buildList {
        chatMessageRepository.getMessageReactions(chatId, msgId).forEach { reaction ->
            val count = chatMessageRepository.getMessageReactionCount(chatId, msgId, reaction)
            val userHandles = chatMessageRepository.getReactionUsers(chatId, msgId, reaction)
            val hasMe = myUserHandle in userHandles
            add(Reaction(reaction, count, userHandles, hasMe))
        }
    }
}