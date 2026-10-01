package mega.privacy.android.domain.usecase.chat

import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.chat.ChatRoomChange
import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use case for monitoring chat retention time update.
 */
class MonitorChatRetentionTimeUpdateUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {

    /**
     * Invoke
     *
     * @param chatId The chat id to monitor
     */
    operator fun invoke(chatId: Long) = chatRepository.monitorChatRoomUpdates(chatId)
        .filter { it.hasChanged(ChatRoomChange.RetentionTime) }
        .map { it.retentionTime }
}