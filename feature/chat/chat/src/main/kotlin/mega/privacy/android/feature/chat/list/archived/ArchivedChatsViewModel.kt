package mega.privacy.android.feature.chat.list.archived

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.domain.usecase.chat.GetChatsUseCase
import mega.privacy.android.domain.usecase.chat.GetChatsUseCase.ChatRoomType
import mega.privacy.android.feature.chat.list.archived.model.ArchivedChatsUiState
import mega.privacy.android.feature.chat.list.formatter.ChatLastMessageFormatter
import mega.privacy.android.feature.chat.list.mapper.ChatRoomTimestampMapper
import mega.privacy.android.feature.chat.list.mapper.ChatRoomUiItemMapper
import timber.log.Timber
import javax.inject.Inject

/**
 * View model for the archived chats screen, exposing the list of archived chat rooms.
 */
@HiltViewModel
internal class ArchivedChatsViewModel @Inject constructor(
    private val getChatsUseCase: GetChatsUseCase,
    private val chatLastMessageFormatter: ChatLastMessageFormatter,
    private val chatRoomTimestampMapper: ChatRoomTimestampMapper,
    private val chatRoomUiItemMapper: ChatRoomUiItemMapper,
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
}
