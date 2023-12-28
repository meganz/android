package mega.privacy.android.app.presentation.meeting.chat.model

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.insertHeaderItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
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
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MessageListViewModel @Inject constructor(
    private val loadMessagesUseCase: LoadMessagesUseCase,
    private val fetchMessagePageUseCase: FetchMessagePageUseCase,
    private val monitorChatRoomMessagesUseCase: MonitorChatRoomMessagesUseCase,
    private val pagedTypedMessageResultUiMapper: PagedTypedMessageResultUiMapper,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val chatId = MutableStateFlow(savedStateHandle.get<Long?>(Constants.CHAT_ID))

    /**
     * Update chat id
     *
     * @param chatId   Chat id
     */
    fun updateChatId(chatId: Long) {
        this.chatId.value = chatId.takeIf { it != -1L }
    }

    private val pagedFlow = chatId
        .filterNotNull()
        .flatMapLatest { chatId ->
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
        }

    /**
     * Paged messages
     */
    val pagedMessages: Flow<PagingData<UiChatMessage>> = pagedFlow
        .map { pagingData ->
            pagingData.insertHeaderItem(
                item = ChatHeaderMessage()
            )
        }

}