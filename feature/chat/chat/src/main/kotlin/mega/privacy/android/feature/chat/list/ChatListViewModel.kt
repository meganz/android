package mega.privacy.android.feature.chat.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.domain.entity.chat.ChatRoomItem
import mega.privacy.android.domain.usecase.chat.GetChatsUseCase
import mega.privacy.android.domain.usecase.chat.GetChatsUseCase.ChatRoomType
import mega.privacy.android.feature.chat.list.formatter.ChatLastMessageFormatter
import mega.privacy.android.feature.chat.list.mapper.ChatRoomTimestampMapper
import mega.privacy.android.feature.chat.list.mapper.ChatRoomUiItemMapper
import mega.privacy.android.feature.chat.list.menu.ChatRoomMenuItem
import mega.privacy.android.feature.chat.list.menu.toUiItem
import mega.privacy.android.feature.chat.list.model.ChatListTabState
import mega.privacy.android.feature.chat.list.model.ChatListUiState
import mega.privacy.android.feature.chat.list.model.ChatRoomActionUiItem
import mega.privacy.android.feature.chat.list.model.ChatRoomUiItem
import timber.log.Timber
import javax.inject.Inject

/**
 * View model for the chat list screen exposing the Chats and Meetings tabs content.
 */
@HiltViewModel
internal class ChatListViewModel @Inject constructor(
    private val getChatsUseCase: GetChatsUseCase,
    private val chatLastMessageFormatter: ChatLastMessageFormatter,
    private val chatRoomTimestampMapper: ChatRoomTimestampMapper,
    private val chatRoomUiItemMapper: ChatRoomUiItemMapper,
    private val chatRoomMenuItems: Map<Int, @JvmSuppressWildcards ChatRoomMenuItem>,
) : ViewModel() {

    private val queryChannel = Channel<String?>(Channel.CONFLATED)

    /**
     * UI state for the chat list screen.
     */
    val uiState: StateFlow<ChatListUiState> by lazy(LazyThreadSafetyMode.NONE) {
        combine(
            chatRoomsFlow(ChatRoomType.NON_MEETINGS),
            chatRoomsFlow(ChatRoomType.MEETINGS),
            queryChannel.receiveAsFlow().onStart { emit(null) },
        ) { chats, meetings, query ->
            ChatListUiState.Data(
                chats = tabState(chats, query),
                meetings = tabState(meetings, query),
            )
        }.catch { e ->
            Timber.e(e, "Failed to load chat list")
        }.asUiStateFlow(
            viewModelScope,
            ChatListUiState.Loading,
        )
    }

    /**
     * Update the active search query, filtering both tabs within the lazy [uiState] composition.
     *
     * @param query Search text, or `null` when search is dismissed.
     */
    fun onSearchQueryChange(query: String?) {
        viewModelScope.launch { queryChannel.send(query) }
    }

    /**
     * Resolve the ordered actions available for the chat with [chatId] when its actions bottom
     * sheet is opened, by filtering the contributed [ChatRoomMenuItem]s by their own eligibility.
     *
     * @param chatId Chat id whose actions are requested.
     * @return Ordered, renderable actions, or empty when the chat is unknown.
     */
    suspend fun getChatRoomActions(chatId: Long): List<ChatRoomActionUiItem> {
        val room = currentChatRooms()[chatId] ?: return emptyList()
        return chatRoomMenuItems.entries
            .sortedBy { it.key }
            .map { it.value }
            .filter { it.shouldDisplay(room) }
            .map { it.toUiItem(room) }
    }

    /**
     * Run the action identified by [actionId] on the chat with [chatId].
     *
     * @param chatId Chat id the action was picked for.
     * @param actionId Test-tag identity of the picked action.
     */
    fun onChatRoomActionSelected(chatId: Long, actionId: String) {
        viewModelScope.launch {
            val room = currentChatRooms()[chatId] ?: return@launch
            val menuItem =
                chatRoomMenuItems.values.firstOrNull { it.testTag == actionId } ?: return@launch
            runCatching { menuItem.onClick(room) }
                .onFailure { Timber.e(it, "Failed to run action $actionId on chat $chatId") }
        }
    }

    private suspend fun currentChatRooms(): Map<Long, ChatRoomItem> = runCatching {
        combine(
            domainChatRoomsFlow(ChatRoomType.NON_MEETINGS),
            domainChatRoomsFlow(ChatRoomType.MEETINGS),
        ) { chats, meetings ->
            (chats + meetings).associateBy(ChatRoomItem::chatId)
        }.first()
    }.getOrElse { e ->
        Timber.e(e, "Failed to load chat rooms for actions")
        emptyMap()
    }

    private fun domainChatRoomsFlow(chatRoomType: ChatRoomType): Flow<List<ChatRoomItem>> =
        getChatsUseCase(
            chatRoomType = chatRoomType,
            lastMessage = { "" },
            lastTimeMapper = { "" },
            meetingTimeMapper = { _, _ -> "" },
            headerTimeMapper = { _, _ -> null },
        )

    private fun chatRoomsFlow(chatRoomType: ChatRoomType): Flow<ImmutableList<ChatRoomUiItem>> =
        getChatsUseCase(
            chatRoomType = chatRoomType,
            lastMessage = chatLastMessageFormatter::invoke,
            lastTimeMapper = chatRoomTimestampMapper::getLastTimeFormatted,
            meetingTimeMapper = chatRoomTimestampMapper::getMeetingTimeFormatted,
            headerTimeMapper = { _, _ -> null },
        ).map { items ->
            items.map(chatRoomUiItemMapper::invoke).toImmutableList()
        }.catch { e ->
            Timber.e(e, "Failed to load $chatRoomType chat rooms")
        }

    private fun tabState(
        items: ImmutableList<ChatRoomUiItem>,
        query: String?,
    ): ChatListTabState {
        val trimmedQuery = query?.trim().orEmpty()
        if (trimmedQuery.isEmpty()) {
            return if (items.isEmpty()) {
                ChatListTabState.Empty
            } else {
                ChatListTabState.Results(items)
            }
        }
        val filtered = items.filter { it.matches(trimmedQuery) }.toImmutableList()
        return if (filtered.isEmpty()) {
            ChatListTabState.NoSearchResults
        } else {
            ChatListTabState.Results(filtered)
        }
    }

    private fun ChatRoomUiItem.matches(query: String): Boolean =
        title.contains(query, ignoreCase = true)
                || lastMessage?.contains(query, ignoreCase = true) == true
}
