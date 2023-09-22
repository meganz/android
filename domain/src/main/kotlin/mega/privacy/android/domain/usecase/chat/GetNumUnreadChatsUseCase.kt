package mega.privacy.android.domain.usecase.chat

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import mega.privacy.android.domain.entity.chat.ChatListItemChanges
import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use case for getting the number of unread chats for the logged in user.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GetNumUnreadChatsUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {

    /**
     * Invoke.
     */
    operator fun invoke(): Flow<Int> =
        flow {
            emit(chatRepository.getNumUnreadChats())
            emitAll(monitorUnreadUpdates())
        }

    /**
     * Monitors the updates to the chat list items and retrieves the latest unread count
     *
     * @return  A [Flow] that emits the updated number of unread chats
     */
    private fun monitorUnreadUpdates(): Flow<Int> =
        chatRepository.monitorChatListItemUpdates()
            .filter { it.changes == ChatListItemChanges.UnreadCount }
            .mapLatest { chatRepository.getNumUnreadChats() }
}
