package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.entity.chat.ChatHistoryLoadStatus
import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use case for loading messages.
 *
 * @property chatRepository [ChatRepository]
 */
class LoadMessagesUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {

    /**
     * Invoke.
     *
     * @param chatId Chat identifier.
     * @return [ChatHistoryLoadStatus].
     */
    suspend operator fun invoke(chatId: Long) =
        chatRepository.loadMessages(chatId, NUMBER_MESSAGES_TO_LOAD)

    companion object {
        /**
         * Number of messages to load
         */
        const val NUMBER_MESSAGES_TO_LOAD = 32
    }
}