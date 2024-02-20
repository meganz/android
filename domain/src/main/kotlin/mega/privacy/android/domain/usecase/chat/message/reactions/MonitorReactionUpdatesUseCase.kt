package mega.privacy.android.domain.usecase.chat.message.reactions

import mega.privacy.android.domain.entity.chat.messages.reactions.Reaction
import mega.privacy.android.domain.entity.chat.messages.reactions.ReactionUpdate
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import javax.inject.Inject

/**
 * Use case for monitoring reaction updates of a chat.
 */
class MonitorReactionUpdatesUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val chatMessageRepository: ChatMessageRepository,
) {
    /**
     * Invoke.
     */
    suspend operator fun invoke(chatId: Long) {
        chatRepository.monitorReactionUpdates(chatId).collect { reactionUpdate ->
            with(reactionUpdate) {
                with(chatMessageRepository.getReactionsFromMessage(chatId, msgId).toMutableList()) {
                    removeIf { it.reaction == reaction }
                    if (hasReaction()) {
                        add(createReaction(chatId))
                    }
                    chatMessageRepository.updateReactionsInMessage(chatId, msgId, this)
                }
            }
        }
    }

    private fun ReactionUpdate.hasReaction() = count > 0

    private suspend fun ReactionUpdate.createReaction(
        chatId: Long,
    ): Reaction {
        val userHandles =
            chatMessageRepository.getReactionUsers(chatId, msgId, reaction)
        val hasMe = chatRepository.getMyUserHandle() in userHandles
        return Reaction(reaction, count, userHandles, hasMe)
    }

}