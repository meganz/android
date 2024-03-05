package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.room.update.HistoryTruncatedByRetentionTime
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import mega.privacy.android.domain.usecase.chat.message.paging.SaveChatMessagesUseCase
import javax.inject.Inject

/**
 * Monitor chat room message updates use case
 *
 * @property chatRepository
 * @property saveChatMessagesUseCase
 * @constructor Create empty Monitor chat room message updates use case
 */
class MonitorChatRoomMessageUpdatesUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val saveChatMessagesUseCase: SaveChatMessagesUseCase,
    private val chatMessageRepository: ChatMessageRepository,
) {
    /**
     * Invoke
     *
     * @param chatId
     */
    suspend operator fun invoke(chatId: Long) {
        chatRepository.monitorMessageUpdates(chatId)
            .collect {
                if (it is HistoryTruncatedByRetentionTime) {
                    chatMessageRepository.truncateMessages(chatId, it.message.timestamp)
                } else {
                    saveChatMessagesUseCase(chatId, listOf(it.message))
                }
            }
    }
}