package mega.privacy.android.app.presentation.meeting.chat.model

import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertHeaderItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import mega.privacy.android.app.presentation.meeting.chat.mapper.PagedTypedMessageResultUiMapper
import mega.privacy.android.app.presentation.meeting.chat.model.messages.UiChatMessage
import mega.privacy.android.app.presentation.meeting.chat.model.messages.header.ChatHeaderMessage
import mega.privacy.android.app.presentation.meeting.chat.model.paging.ChatMessagePagingSource
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.usecase.chat.message.FetchMessagePageUseCase
import mega.privacy.android.domain.usecase.chat.message.MonitorChatRoomMessagesUseCase
import mega.privacy.android.domain.usecase.meeting.LoadMessagesUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Message list view model
 *
 * @property loadMessagesUseCase
 * @property fetchMessagePageUseCase
 * @property monitorChatRoomMessagesUseCase
 * @property pagedTypedMessageResultUiMapper
 *
 * @param savedStateHandle
 */
@HiltViewModel
class MessageListViewModel @Inject constructor(
    private val loadMessagesUseCase: LoadMessagesUseCase,
    private val fetchMessagePageUseCase: FetchMessagePageUseCase,
    private val monitorChatRoomMessagesUseCase: MonitorChatRoomMessagesUseCase,
    private val pagedTypedMessageResultUiMapper: PagedTypedMessageResultUiMapper,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val chatId = savedStateHandle.get<Long?>(Constants.CHAT_ID) ?: -1

    /**
     * Latest message time
     */
    val latestMessageId = mutableLongStateOf(-1L)

    /**
     * Asked enable rich link
     */
    val askedEnableRichLink = mutableStateOf(false)

    private val pagedFlow =
        Pager(
            PagingConfig(
                pageSize = 32,
                prefetchDistance = 10
            )
        ) {
            ChatMessagePagingSource(
                chatId = chatId,
                loadMessages = loadMessagesUseCase,
                fetchMessages = fetchMessagePageUseCase,
                scope = viewModelScope,
                messageFlow = monitorChatRoomMessagesUseCase(chatId).shareIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                ).onEach {
                    Timber.d("Paging monitorChatRoomMessagesUseCase returned with message: $it")
                },
                pagedTypedMessageResultUiMapper = pagedTypedMessageResultUiMapper,
            )
        }.flow
            .cachedIn(viewModelScope)


    /**
     * Paged messages
     */
    val pagedMessages: Flow<PagingData<UiChatMessage>> = pagedFlow
        .map { pagingData ->
            pagingData.insertHeaderItem(
                item = ChatHeaderMessage()
            )
        }

    /**
     * Update latest message id
     *
     * @param id
     */
    fun updateLatestMessageId(id: Long) {
        latestMessageId.longValue = id
    }

    /**
     * On asked enable rich link
     *
     */
    fun onAskedEnableRichLink() {
        askedEnableRichLink.value = true
    }
}