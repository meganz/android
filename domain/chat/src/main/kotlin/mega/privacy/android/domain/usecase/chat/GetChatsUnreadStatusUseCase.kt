package mega.privacy.android.domain.usecase.chat

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import mega.privacy.android.domain.entity.chat.ChatListItemChanges
import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use case to get Chats and Meetings unread item updates
 *
 * @property chatRepository [ChatRepository]
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GetChatsUnreadStatusUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {

    /**
     * Retrieve the unread status of chats and meetings.
     *
     * @return A [Flow] that emits a pair of booleans representing unread chats and meetings.
     */
    operator fun invoke(): Flow<Pair<Boolean, Boolean>> =
        flow {
            emit(hasUnreadChats().await() to hasUnreadMeetings().await())
            emitAll(monitorUnreadUpdates())
        }

    /**
     * Checks if there are any non-meeting chat rooms with unread messages.
     *
     * @return True if there are non-meeting chat rooms with unread messages, false otherwise.
     */
    private suspend fun hasUnreadChats(): Deferred<Boolean> = coroutineScope {
        async {
            chatRepository.getUnreadNonMeetingChatListItems().any()
        }
    }

    /**
     * Checks if there are any meeting chat rooms with unread messages.
     *
     * @return True if there are meeting chat rooms with unread messages, false otherwise.
     */
    private suspend fun hasUnreadMeetings(): Deferred<Boolean> = coroutineScope {
        async {
            chatRepository.getUnreadMeetingChatListItems().any()
        }
    }

    /**
     * Monitors the updates to the chat list items and retrieves the unread status of chats and meetings.
     *
     * @return A [Flow] that emits pairs of booleans representing the updated unread status of chats and meetings.
     */
    private fun monitorUnreadUpdates(): Flow<Pair<Boolean, Boolean>> =
        chatRepository.monitorChatListItemUpdates()
            .filter { it.changes == ChatListItemChanges.UnreadCount }
            .mapLatest { hasUnreadChats().await() to hasUnreadMeetings().await() }
            .distinctUntilChanged()
}
