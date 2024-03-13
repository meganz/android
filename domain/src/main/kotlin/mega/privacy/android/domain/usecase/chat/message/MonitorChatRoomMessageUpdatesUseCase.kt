package mega.privacy.android.domain.usecase.chat.message

import kotlinx.coroutines.flow.onEach
import mega.privacy.android.domain.entity.chat.ChatMessageType
import mega.privacy.android.domain.entity.chat.room.update.ChatRoomMessageUpdate
import mega.privacy.android.domain.entity.chat.room.update.HistoryTruncatedByRetentionTime
import mega.privacy.android.domain.entity.chat.room.update.MessageReceived
import mega.privacy.android.domain.entity.chat.room.update.MessageUpdate
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import mega.privacy.android.domain.usecase.chat.message.paging.ClearChatMessagesUseCase
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
    private val clearChatMessagesUseCase: ClearChatMessagesUseCase,
) {
    /**
     * Invoke
     *
     * @param chatId
     * @param onEvent callback to be called when a message update is received and before saving it to the database
     */
    suspend operator fun invoke(chatId: Long, onEvent: (ChatRoomMessageUpdate) -> Unit) {
        chatRepository.monitorMessageUpdates(chatId)
            .onEach {
                onEvent(it)
            }.collect {
                when (it) {
                    is HistoryTruncatedByRetentionTime -> {
                        chatMessageRepository.truncateMessages(chatId, it.message.timestamp)
                    }

                    is MessageUpdate -> {
                        if (it.message.type == ChatMessageType.TRUNCATE) {
                            clearChatMessagesUseCase(chatId = chatId, clearPendingMessages = true)
                        }
                        saveChatMessagesUseCase(chatId, listOf(it.message))
                    }

                    is MessageReceived -> {
                        saveChatMessagesUseCase(chatId, listOf(it.message))
                    }
                }
            }
    }
}