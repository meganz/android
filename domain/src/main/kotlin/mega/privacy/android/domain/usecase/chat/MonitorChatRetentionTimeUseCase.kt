package mega.privacy.android.domain.usecase.chat

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import mega.privacy.android.domain.usecase.GetChatRoomUseCase
import javax.inject.Inject

/**
 * Monitor the retention time of a chat room, emitting the current value first and then every
 * update. A disabled retention time (0) is emitted as null.
 */
class MonitorChatRetentionTimeUseCase @Inject constructor(
    private val getChatRoomUseCase: GetChatRoomUseCase,
    private val monitorChatRetentionTimeUpdateUseCase: MonitorChatRetentionTimeUpdateUseCase,
) {
    /**
     * Invoke.
     *
     * @param chatId Id of the chat room.
     * @return Flow of the retention time in seconds, or null when disabled.
     */
    operator fun invoke(chatId: Long): Flow<Long?> =
        monitorChatRetentionTimeUpdateUseCase(chatId)
            .onStart { emit(getChatRoomUseCase(chatId)?.retentionTime ?: 0L) }
            .map { retentionTime -> retentionTime.takeIf { it > 0L } }
}
