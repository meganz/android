package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.chat.ChatRoom

/**
 * Use case for monitoring updates on chat room item
 */
fun interface MonitorChatRoomUpdates {

    /**
     * Invoke.
     *
     * @param chatId    Chat id.
     * @return          Flow of [ChatRoom].
     */
    suspend operator fun invoke(chatId: Long): Flow<ChatRoom>
}