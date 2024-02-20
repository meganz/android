package mega.privacy.android.app.presentation.meeting.chat.model

import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.TerminalSeparatorType
import androidx.paging.cachedIn
import androidx.paging.insertFooterItem
import androidx.paging.insertSeparators
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.di.meeting.chat.paging.PagedChatMessageRemoteMediatorFactory
import mega.privacy.android.app.presentation.meeting.chat.mapper.ChatMessageDateSeparatorMapper
import mega.privacy.android.app.presentation.meeting.chat.mapper.UiChatMessageMapper
import mega.privacy.android.app.presentation.meeting.chat.model.messages.UiChatMessage
import mega.privacy.android.app.presentation.meeting.chat.model.messages.header.ChatHeaderMessage
import mega.privacy.android.app.presentation.meeting.chat.model.messages.header.ChatUnreadHeaderMessage
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.usecase.chat.message.GetLastMessageSeenIdUseCase
import mega.privacy.android.domain.usecase.chat.message.MonitorChatRoomMessageUpdatesUseCase
import mega.privacy.android.domain.usecase.chat.message.SetMessageSeenUseCase
import mega.privacy.android.domain.usecase.chat.message.paging.GetChatPagingSourceUseCase
import mega.privacy.android.domain.usecase.chat.message.reactions.MonitorReactionUpdatesUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Message list view model
 *
 * @property uiChatMessageMapper
 * @property getChatPagingSourceUseCase
 * @constructor
 *
 * @param remoteMediatorFactory
 * @param savedStateHandle
 */
@OptIn(ExperimentalPagingApi::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class MessageListViewModel @Inject constructor(
    private val uiChatMessageMapper: UiChatMessageMapper,
    private val getChatPagingSourceUseCase: GetChatPagingSourceUseCase,
    private val chatMessageDateSeparatorMapper: ChatMessageDateSeparatorMapper,
    remoteMediatorFactory: PagedChatMessageRemoteMediatorFactory,
    savedStateHandle: SavedStateHandle,
    private val getLastMessageSeenIdUseCase: GetLastMessageSeenIdUseCase,
    private val setMessageSeenUseCase: SetMessageSeenUseCase,
    private val monitorChatRoomMessageUpdatesUseCase: MonitorChatRoomMessageUpdatesUseCase,
    private val monitorReactionUpdatesUseCase: MonitorReactionUpdatesUseCase,
) : ViewModel() {

    private val chatId = savedStateHandle.get<Long?>(Constants.CHAT_ID) ?: -1

    private val unreadCount = MutableStateFlow<Int?>(null)

    /**
     * Latest message time
     */
    val latestMessageId = mutableLongStateOf(-1L)

    /**
     * Asked enable rich link
     */
    val askedEnableRichLink = mutableStateOf(false)

    private val _state = MutableStateFlow(MessageListUiState())

    /**
     * State
     */
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching {
                getLastMessageSeenIdUseCase(chatId)
            }.onSuccess { lastSeenMessageId ->
                _state.update { it.copy(lastSeenMessageId = lastSeenMessageId) }
            }.onFailure {
                Timber.e(it, "Failed to get last seen message id")
            }
        }

        viewModelScope.launch {
            runCatching { monitorChatRoomMessageUpdatesUseCase(chatId) }
                .onFailure {
                    Timber.e(it, "Monitor message updates threw an exception")
                }
        }

        viewModelScope.launch {
            runCatching { monitorReactionUpdatesUseCase(chatId) }
                .onFailure {
                    Timber.e(it, "Monitor reaction updates threw an exception")
                }
        }
    }

    private val pagedFlow = unreadCount
        .filterNotNull()
        .flatMapLatest { unreadCount ->
            Pager(
                config = PagingConfig(
                    pageSize = 32,
                    initialLoadSize = unreadCount.coerceAtLeast(32),
                ),
                remoteMediator = remoteMediatorFactory.create(
                    chatId = chatId,
                    coroutineScope = viewModelScope,
                ),
            ) {
                getChatPagingSourceUseCase(chatId)
            }.flow.map { pagingData ->
                pagingData.map {
                    uiChatMessageMapper(it)
                }.insertSeparators { before, after: UiChatMessage? ->
                    chatMessageDateSeparatorMapper(
                        firstMessage = after,
                        secondMessage = before
                    ) //Messages are passed in reverse order as the list is reversed in the ui

                    if (unreadCount > 0
                        && after?.id == state.value.lastSeenMessageId
                        && state.value.lastSeenMessageId != -1L
                    ) {
                        ChatUnreadHeaderMessage(unreadCount)
                    } else {
                        null
                    }
                }.insertFooterItem(
                    item = ChatHeaderMessage(),
                    terminalSeparatorType = TerminalSeparatorType.SOURCE_COMPLETE
                )
            }
        }.cachedIn(viewModelScope)

    /**
     * Paged messages
     */
    val pagedMessages: Flow<PagingData<UiChatMessage>> = pagedFlow

    /**
     * Set unread count
     *
     * @param count Unread count
     */
    fun setUnreadCount(count: Int) {
        // Only set unread count if it's not set yet
        if (unreadCount.value == null) {
            unreadCount.value = count
        }
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

    /**
     * On scrolled to last seen message
     *
     */
    fun onScrolledToLastSeenMessage() {
        _state.update { it.copy(isJumpingToLastSeenMessage = true) }
    }

    /**
     * Set message seen
     *
     * @param messageId Message id
     */
    fun setMessageSeen(messageId: Long) {
        viewModelScope.launch {
            runCatching {
                setMessageSeenUseCase(chatId, messageId)
            }.onFailure {
                Timber.e(it, "Failed to set message seen")
            }
        }
    }
}