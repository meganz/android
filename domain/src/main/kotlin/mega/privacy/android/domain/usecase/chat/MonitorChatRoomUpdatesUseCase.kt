package mega.privacy.android.domain.usecase.chat

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use case for monitoring updates on chat room item
 */
class MonitorChatRoomUpdatesUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {

    /**
     * Invocation method.
     *
     * @param chatId    Chat id.
     * @return          Flow of [ChatRoom].
     */
    operator fun invoke(chatId: Long): Flow<ChatRoom> =
        chatRepository.monitorChatRoomUpdates(chatId = chatId)
}
