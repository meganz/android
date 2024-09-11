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
import androidx.paging.cachedIn
import androidx.paging.insertHeaderItem
import androidx.paging.insertSeparators
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.di.meeting.chat.paging.PagedChatMessageRemoteMediatorFactory
import mega.privacy.android.app.presentation.meeting.chat.mapper.ChatMessageDateSeparatorMapper
import mega.privacy.android.app.presentation.meeting.chat.mapper.ChatMessageTimeSeparatorMapper
import mega.privacy.android.app.presentation.meeting.chat.mapper.UiChatMessageMapper
import mega.privacy.android.app.presentation.meeting.chat.model.messages.UiChatMessage
import mega.privacy.android.app.presentation.meeting.chat.model.messages.header.ChatUnreadHeaderMessage
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.compose.ChatArgs
import mega.privacy.android.domain.entity.chat.ChatMessageStatus
import mega.privacy.android.domain.entity.chat.ChatMessageType
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.chat.room.update.MessageReceived
import mega.privacy.android.domain.entity.chat.room.update.MessageUpdate
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.MonitorContactCacheUpdates
import mega.privacy.android.domain.usecase.chat.message.MonitorChatRoomMessageUpdatesUseCase
import mega.privacy.android.domain.usecase.chat.message.MonitorPendingMessagesUseCase
import mega.privacy.android.domain.usecase.chat.message.SetMessageSeenUseCase
import mega.privacy.android.domain.usecase.chat.message.paging.GetChatPagingSourceUseCase
import mega.privacy.android.domain.usecase.chat.message.reactions.MonitorReactionUpdatesUseCase
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.DurationUnit
import kotlin.time.toDuration

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
@OptIn(ExperimentalPagingApi::class, ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class MessageListViewModel @Inject constructor(
    private val uiChatMessageMapper: UiChatMessageMapper,
    private val getChatPagingSourceUseCase: GetChatPagingSourceUseCase,
    private val chatMessageDateSeparatorMapper: ChatMessageDateSeparatorMapper,
    private val chatMessageTimeSeparatorMapper: ChatMessageTimeSeparatorMapper,
    remoteMediatorFactory: PagedChatMessageRemoteMediatorFactory,
    savedStateHandle: SavedStateHandle,
    private val setMessageSeenUseCase: SetMessageSeenUseCase,
    private val monitorChatRoomMessageUpdatesUseCase: MonitorChatRoomMessageUpdatesUseCase,
    private val monitorReactionUpdatesUseCase: MonitorReactionUpdatesUseCase,
    private val monitorContactCacheUpdates: MonitorContactCacheUpdates,
    monitorPendingMessagesUseCase: MonitorPendingMessagesUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val conversationArgs = ChatArgs(savedStateHandle)
    private val chatId = conversationArgs.chatId

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

    private var lastNotSeenMessage: TypedMessage? = null
    private var hideUnreadCount: Boolean = false

    /**
     * State
     */
    val state = _state.asStateFlow()

    init {
        monitorContactCacheUpdate()
        monitorMessageUpdates()
        monitorReactionUpdates()
    }

    private fun monitorContactCacheUpdate() {
        viewModelScope.launch {
            monitorContactCacheUpdates()
                // I don't know why sdk emit 2 the same events, add debounce to optimize
                .debounce(300L.toDuration(DurationUnit.MILLISECONDS))
                .catch { Timber.e(it) }
                .collect {
                    Timber.d("Contact cache update: $it")
                    _state.update { state -> state.copy(userUpdate = it) }
                }
        }
    }

    private fun monitorReactionUpdates() {
        viewModelScope.launch {
            runCatching { monitorReactionUpdatesUseCase(chatId) }
                .onFailure {
                    Timber.e(it, "Monitor reaction updates threw an exception")
                }
        }
    }

    private fun monitorMessageUpdates() {
        viewModelScope.launch {
            runCatching {
                monitorChatRoomMessageUpdatesUseCase(chatId) {
                    if (it is MessageReceived) {
                        _state.update { state ->
                            state.copy(
                                receivedMessages = state.receivedMessages + it.message.messageId,
                                extraUnreadCount = state.extraUnreadCount + 1
                            )
                        }
                        setMessageSeen(it.message.messageId)
                    } else if (it is MessageUpdate) {
                        if (it.message.type == ChatMessageType.TRUNCATE) {
                            lastNotSeenMessage = null
                            hideUnreadCount = true
                        }
                    }
                }
            }.onFailure {
                Timber.e(it, "Monitor message updates threw an exception")
            }
        }
    }

    private val remoteMediator = remoteMediatorFactory.create(
        chatId = chatId,
        coroutineScope = viewModelScope,
    )

    private val pagedFlow = unreadCount
        .filterNotNull()
        .flatMapLatest { unreadCount ->
            combine(
                // 1- paged messages
                Pager(
                    config = PagingConfig(
                        pageSize = 32,
                        initialLoadSize = unreadCount.coerceAtLeast(32 * 3), // recommend to load at least 3 pages
                    ),
                    remoteMediator = remoteMediator,
                ) {
                    getChatPagingSourceUseCase(chatId)
                }.flow.cachedIn(viewModelScope), //this cachedIn is needed to avoid to collect twice from pageEventFlow (which is illegal) within the combine operator
                // 2- pending messages
                monitorPendingMessagesUseCase(chatId).map { pendingMessages ->
                    pendingMessages.map { pendingMessage ->
                        uiChatMessageMapper(
                            pendingMessage
                        )
                    }
                }
            ) { pagingData, pendingMessages ->
                pagingData.map {
                    if (!hideUnreadCount
                        && it.status == ChatMessageStatus.NOT_SEEN
                        && (lastNotSeenMessage?.time ?: Long.MAX_VALUE) > it.time
                    ) {
                        lastNotSeenMessage = it
                    }
                    uiChatMessageMapper(it)
                }
                    .insertSeparators { before, after ->
                        if (unreadCount > 0
                            && lastNotSeenMessage != null
                            && before?.id == lastNotSeenMessage?.msgId
                            && !hideUnreadCount
                        ) {
                            ChatUnreadHeaderMessage(unreadCount, after?.message)
                        } else {
                            null
                        }
                    }
                    .insertSeparators { before, after: UiChatMessage? ->
                        chatMessageTimeSeparatorMapper(
                            firstMessage = after,
                            secondMessage = before
                        )
                    }
                    .insertSeparators { before, after: UiChatMessage? ->
                        chatMessageDateSeparatorMapper(
                            firstMessage = after,
                            secondMessage = before
                        ) //Messages are passed in reverse order as the list is reversed in the ui
                    }.let { pagingDataWithoutPending ->
                        pendingMessages.fold(pagingDataWithoutPending) { pagingData, pendingMessage ->
                            //pending messages always at the end (header because list is reversed in the ui)
                            pagingData.insertHeaderItem(
                                item = pendingMessage,
                            )
                        }
                    }
            }
        }.flowOn(ioDispatcher)
        .cachedIn(viewModelScope) //this cachedIn is to avoid losing the resulting on rotation

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
     * Update latest message
     *
     * @param messages list of messages reversed order, latest message is the first
     */
    fun updateLatestMessage(messages: List<UiChatMessage?>) {
        if (latestMessageId.longValue == -1L && messages.isNotEmpty()) {
            // mark first time user enter chat room as seen
            messages.find { it?.message?.status == ChatMessageStatus.NOT_SEEN }?.let {
                setMessageSeen(it.id)
            }
        }
        val lastMessage = messages.firstOrNull()?.message
        latestMessageId.longValue = lastMessage?.msgId ?: -1L
        if (lastMessage?.isMine == true) {
            // if user sent a message, reset the extraUnreadCount and remove unread header
            lastNotSeenMessage = null
            hideUnreadCount = true
            _state.update { state -> state.copy(extraUnreadCount = 0) }
        }
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

    /**
     * On user update handled.
     */
    fun onUserUpdateHandled() {
        _state.update { state -> state.copy(userUpdate = null) }
    }

    /**
     * On show all messages
     *
     */
    fun onScrollToLatestMessage() {
        _state.update { state -> state.copy(receivedMessages = emptySet()) }
    }
}