package mega.privacy.android.domain.usecase.chat

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.ChatRoomLastMessage
import mega.privacy.android.domain.entity.chat.ChatListItemChanges
import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use case for monitoring updates on note to self chat
 */
class MonitorNoteToSelfChatIsEmptyUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {

    /**
     * Invoke.
     *
     * @return Flow of [Boolean].
     */
    operator fun invoke(noteToSelfChatId: Long): Flow<Boolean> = flow {
        val lastMsg: ChatRoomLastMessage =
            chatRepository.getChatListItem(noteToSelfChatId)?.lastMessageType
                ?: ChatRoomLastMessage.Invalid

        emit(isEmptyChat(lastMsg = lastMsg))
        emitAll(monitorChatListItemUpdates(noteToSelfChatId))
    }

    /**
     * Check if the chat is empty
     *
     * @param  lastMsg   [ChatRoomLastMessage]
     * @return  True, if the chat is empty. False, if not.
     */
    private fun isEmptyChat(lastMsg: ChatRoomLastMessage): Boolean =
        lastMsg == ChatRoomLastMessage.Invalid || lastMsg == ChatRoomLastMessage.Unknown


    /**
     * Monitor chat list item updates
     */
    private fun monitorChatListItemUpdates(
        chatId: Long,
    ): Flow<Boolean> =
        chatRepository.monitorChatListItemUpdates()
            .filter { item ->
                item.chatId == chatId && item.changes == ChatListItemChanges.LastMessage
            }
            .map { item ->
                return@map isEmptyChat(item.lastMessageType)
            }
}