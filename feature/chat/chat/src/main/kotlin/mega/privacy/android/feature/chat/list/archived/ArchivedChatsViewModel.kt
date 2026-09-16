package mega.privacy.android.feature.chat.list.archived

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.domain.entity.chat.ChatRoomItem
import mega.privacy.android.domain.usecase.chat.GetChatsUseCase
import mega.privacy.android.domain.usecase.chat.GetChatsUseCase.ChatRoomType
import mega.privacy.android.feature.chat.list.archived.model.ArchivedChatsUiState
import mega.privacy.android.feature.chat.list.formatter.ChatLastMessageFormatter
import mega.privacy.android.feature.chat.list.mapper.ChatRoomTimestampMapper
import mega.privacy.android.feature.chat.list.mapper.ChatRoomUiItemMapper
import mega.privacy.android.feature.chat.list.menu.ChatRoomActions
import mega.privacy.android.feature.chat.list.model.ChatRoomActionUiItem
import timber.log.Timber
import javax.inject.Inject

/**
 * View model for the archived chats screen, exposing the list of archived chat rooms and the
 * actions available on them.
 */
@HiltViewModel
internal class ArchivedChatsViewModel @Inject constructor(
    private val getChatsUseCase: GetChatsUseCase,
    private val chatLastMessageFormatter: ChatLastMessageFormatter,
    private val chatRoomTimestampMapper: ChatRoomTimestampMapper,
    private val chatRoomUiItemMapper: ChatRoomUiItemMapper,
    private val chatRoomActions: ChatRoomActions,
) : ViewModel() {

    /**
     * UI state for the archived chats screen.
     */
    val uiState: StateFlow<ArchivedChatsUiState> by lazy(LazyThreadSafetyMode.NONE) {
        getChatsUseCase(
            chatRoomType = ChatRoomType.ARCHIVED_CHATS,
            lastMessage = chatLastMessageFormatter::invoke,
            lastTimeMapper = chatRoomTimestampMapper::getLastTimeFormatted,
            meetingTimeMapper = chatRoomTimestampMapper::getMeetingTimeFormatted,
            headerTimeMapper = { _, _ -> null },
        ).map { items ->
            if (items.isEmpty()) {
                ArchivedChatsUiState.Empty
            } else {
                ArchivedChatsUiState.Data(
                    items.map(chatRoomUiItemMapper::invoke).toImmutableList()
                )
            }
        }.catch { e ->
            Timber.e(e, "Failed to load archived chats")
        }.asUiStateFlow(viewModelScope, ArchivedChatsUiState.Loading)
    }

    /**
     * Resolve the ordered actions available for the archived chats in [chatIds], so an archived
     * chat can be restored to the chat list from here.
     *
     * @param chatIds Ids of the chats the actions sheet was opened for.
     * @return Ordered, renderable actions common to all those chats, or empty when none apply.
     */
    suspend fun getChatRoomActions(chatIds: Set<Long>): List<ChatRoomActionUiItem> =
        chatRoomActions.resolve(selectedRooms(chatIds))

    /**
     * Run the action identified by [actionId] on every archived chat in [chatIds], reporting the
     * outcome to the user.
     *
     * @param chatIds Ids of the chats the action was picked for.
     * @param actionId Test-tag identity of the picked action.
     */
    fun onChatRoomActionSelected(chatIds: Set<Long>, actionId: String) {
        viewModelScope.launch {
            chatRoomActions.run(actionId, selectedRooms(chatIds))
        }
    }

    private suspend fun selectedRooms(chatIds: Set<Long>): List<ChatRoomItem> {
        if (chatIds.isEmpty()) return emptyList()
        val rooms = currentChatRooms()
        return chatIds.mapNotNull { rooms[it] }
    }

    private suspend fun currentChatRooms(): Map<Long, ChatRoomItem> = runCatching {
        domainChatRoomsFlow().first().associateBy(ChatRoomItem::chatId)
    }.getOrElse { e ->
        Timber.e(e, "Failed to load archived chat rooms for actions")
        emptyMap()
    }

    private fun domainChatRoomsFlow(): Flow<List<ChatRoomItem>> = getChatsUseCase(
        chatRoomType = ChatRoomType.ARCHIVED_CHATS,
        lastMessage = { "" },
        lastTimeMapper = { "" },
        meetingTimeMapper = { _, _ -> "" },
        headerTimeMapper = { _, _ -> null },
    )
}
