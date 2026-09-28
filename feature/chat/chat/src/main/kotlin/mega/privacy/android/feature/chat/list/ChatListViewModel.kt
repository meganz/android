package mega.privacy.android.feature.chat.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.domain.entity.chat.ChatPushNotificationMuteOption
import mega.privacy.android.domain.entity.chat.ChatRoomItem
import mega.privacy.android.domain.entity.chat.ChatStatus
import mega.privacy.android.domain.entity.chat.MeetingTooltipItem
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.call.IsParticipatingInChatCallUseCase
import mega.privacy.android.domain.usecase.chat.CreateGroupChatRoomUseCase
import mega.privacy.android.domain.usecase.chat.GetChatMuteOptionListUseCase
import mega.privacy.android.domain.usecase.chat.GetChatsUseCase
import mega.privacy.android.domain.usecase.chat.GetChatsUseCase.ChatRoomType
import mega.privacy.android.domain.usecase.chat.GetCurrentChatStatusUseCase
import mega.privacy.android.domain.usecase.chat.GetMeetingTooltipsUseCase
import mega.privacy.android.domain.usecase.chat.HasArchivedChatsUseCase
import mega.privacy.android.domain.usecase.chat.MonitorChatsDoNotDisturbEnabledUseCase
import mega.privacy.android.domain.usecase.chat.MuteChatPushNotificationUseCase
import mega.privacy.android.domain.usecase.chat.SetCurrentUserStatusUseCase
import mega.privacy.android.domain.usecase.chat.SetNextMeetingTooltipUseCase
import mega.privacy.android.domain.usecase.chat.StartConversationUseCase
import mega.privacy.android.domain.usecase.contact.GetContactHandleUseCase
import mega.privacy.android.domain.usecase.MonitorChatListItemUpdates
import mega.privacy.android.domain.usecase.contact.MonitorHasAnyContactUseCase
import mega.privacy.android.feature.chat.list.formatter.ChatLastMessageFormatter
import mega.privacy.android.feature.chat.list.mapper.ChatRoomTimestampMapper
import mega.privacy.android.feature.chat.list.mapper.ChatRoomUiItemMapper
import mega.privacy.android.feature.chat.list.menu.ChatRoomActions
import mega.privacy.android.feature.chat.list.menu.OfferedChatRoomActions
import mega.privacy.android.feature.chat.list.model.ChatListTabState
import mega.privacy.android.feature.chat.list.model.ChatListUiState
import mega.privacy.android.feature.chat.list.model.ChatRoomActionUiItem
import mega.privacy.android.feature.chat.list.model.ChatRoomUiItem
import mega.privacy.android.feature.chat.list.model.MeetingAction
import mega.privacy.android.feature.chat.list.model.MeetingActionEvent
import mega.privacy.android.feature.chat.runCatchingCancellable
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import mega.privacy.android.navigation.destination.NewChatNavKey
import mega.privacy.android.shared.resources.R as sharedR
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
    private val getContactHandleUseCase: GetContactHandleUseCase,
    private val startConversationUseCase: StartConversationUseCase,
    private val createGroupChatRoomUseCase: CreateGroupChatRoomUseCase,
    private val isParticipatingInChatCallUseCase: IsParticipatingInChatCallUseCase,
    private val monitorHasAnyContactUseCase: MonitorHasAnyContactUseCase,
    private val monitorChatListItemUpdates: MonitorChatListItemUpdates,
    private val hasArchivedChatsUseCase: HasArchivedChatsUseCase,
    private val getCurrentChatStatusUseCase: GetCurrentChatStatusUseCase,
    private val setCurrentUserStatusUseCase: SetCurrentUserStatusUseCase,
    private val getMeetingTooltipsUseCase: GetMeetingTooltipsUseCase,
    private val setNextMeetingTooltipUseCase: SetNextMeetingTooltipUseCase,
    private val getChatMuteOptionListUseCase: GetChatMuteOptionListUseCase,
    private val muteChatPushNotificationUseCase: MuteChatPushNotificationUseCase,
    private val monitorChatsDoNotDisturbEnabledUseCase: MonitorChatsDoNotDisturbEnabledUseCase,
    private val snackbarEventQueue: SnackbarEventQueue,
    private val chatRoomActions: ChatRoomActions,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : ViewModel() {

    private val queryChannel = Channel<String?>(Channel.CONFLATED)

    private val tooltipChannel = Channel<Unit>(Channel.CONFLATED)

    private val openChatChannel = Channel<Long>(Channel.BUFFERED)

    private val meetingActionChannel = Channel<MeetingActionEvent>(Channel.BUFFERED)

    private var offeredActions: OfferedChatRoomActions? = null

    /**
     * Emits the chat id of a chat that has just been created through the new-chat flow and should
     * be opened.
     */
    val openChatEvents: Flow<Long> = openChatChannel.receiveAsFlow()

    /**
     * Emits the resolved outcome of a picked meeting action.
     */
    val meetingActionEvents: Flow<MeetingActionEvent> = meetingActionChannel.receiveAsFlow()

    /**
     * UI state for the chat list screen.
     */
    val uiState: StateFlow<ChatListUiState> by lazy(LazyThreadSafetyMode.NONE) {
        combine(
            chatRoomsFlow(),
            queryChannel.receiveAsFlow().onStart { emit(null) },
            hasAnyContactFlow(),
            toolbarFlow(),
            meetingTooltipFlow(),
        ) { rooms, query, hasAnyContact, (chatStatus, isDoNotDisturbEnabled), tooltip ->
            ChatListUiState.Data(
                chats = tabState(rooms.chats, query),
                meetings = tabState(rooms.meetings, query),
                chatsHaveUnread = rooms.chats.anyUnread(),
                meetingsHaveUnread = rooms.meetings.anyUnread(),
                hasAnyContact = hasAnyContact,
                hasArchivedChats = rooms.hasArchived,
                isSearchAvailable = rooms.isSearchable,
                chatStatus = chatStatus,
                isDoNotDisturbEnabled = isDoNotDisturbEnabled,
                meetingTooltip = tooltip,
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
     * Create the chat chosen through the new-chat flow and, on success, emit its id through
     * [openChatEvents] so the screen can open the conversation.
     *
     * A [NewChatNavKey.NewChatResult] with null group settings and a single email creates (or
     * reuses) a 1:1 chat; a non-null group settings creates a group chat with the chosen options.
     *
     * @param result The selection returned by the new-chat picker.
     */
    fun onNewChatResult(result: NewChatNavKey.NewChatResult) {
        viewModelScope.launch {
            runCatchingCancellable {
                val groupSettings = result.groupSettings
                if (groupSettings == null) {
                    val handle = result.emails.firstOrNull()
                        ?.let { getContactHandleUseCase(it) }
                        ?: error("No contact handle for the selected email")
                    startConversationUseCase(isGroup = false, userHandles = listOf(handle))
                } else {
                    createGroupChatRoomUseCase(
                        emails = result.emails,
                        title = groupSettings.title,
                        isEkr = groupSettings.isEkr,
                        addParticipants = groupSettings.allowAddParticipants,
                        chatLink = groupSettings.isChatLink,
                    )
                }
            }.onSuccess { chatId ->
                openChatChannel.send(chatId)
            }.onFailure {
                Timber.e(it, "Failed to create chat from new-chat result")
            }
        }
    }

    /**
     * Resolve the meeting [action] picked from the meetings bottom sheet or the empty Meetings tab
     * and emit the outcome through [meetingActionEvents].
     *
     * Starting or joining a meeting is refused while the user is already participating in a call,
     * matching the legacy chat list; scheduling is always allowed.
     *
     * @param action The action the user picked.
     */
    fun onMeetingActionSelected(action: MeetingAction) {
        viewModelScope.launch {
            val event = when (action) {
                MeetingAction.Schedule -> MeetingActionEvent.ScheduleMeeting

                MeetingAction.Start -> if (isParticipatingInACall()) {
                    MeetingActionEvent.StartBlockedByOngoingCall
                } else {
                    MeetingActionEvent.StartMeeting
                }

                MeetingAction.Join -> if (isParticipatingInACall()) {
                    MeetingActionEvent.JoinBlockedByOngoingCall
                } else {
                    MeetingActionEvent.JoinMeeting
                }
            }
            meetingActionChannel.send(event)
        }
    }

    private suspend fun isParticipatingInACall(): Boolean =
        runCatchingCancellable { isParticipatingInChatCallUseCase() }
            .getOrElse { e ->
                Timber.e(e, "Failed to check whether the user is in a call")
                false
            }

    /**
     * Resolve the ordered actions available for the chats in [chatIds] when the actions bottom
     * sheet is opened, by filtering the contributed chat room actions to those eligible for every
     * chat. A single-chat selection therefore yields exactly that chat's actions.
     *
     * The chat rooms read here are kept as the offer the sheet was rendered from, so
     * [onBulkChatRoomActionSelected] applies the picked action to those same chat rooms rather
     * than to a second, possibly disagreeing, read.
     *
     * @param chatIds Ids of the chats the sheet was opened for.
     * @return Ordered, renderable actions common to all those chats, or empty when none apply.
     */
    suspend fun getBulkChatRoomActions(chatIds: Set<Long>): List<ChatRoomActionUiItem> {
        val rooms = selectedRooms(chatIds)
        val actions = chatRoomActions.resolve(rooms)
        offeredActions = OfferedChatRoomActions.of(chatIds, rooms, actions)
        return actions
    }

    /**
     * Run the action identified by [actionId] on the chat rooms [getBulkChatRoomActions] resolved
     * for [chatIds], reporting the outcome to the user.
     *
     * Acting on that one read is what makes the action honest: it reaches exactly the chats whose
     * eligibility put it in the sheet, and an action that presents differently per chat applies
     * the direction its label promised, however the chats have changed since. A second read could
     * disagree with the first on both counts.
     *
     * The action itself runs in the application scope rather than [viewModelScope]: archiving,
     * leaving, clearing and muting change the chats for good, so leaving the screen mid-run must
     * not abandon the batch part-way through or drop the message telling the user what happened.
     * Reading the chat rooms stays out of that process-wide scope, so neither this ViewModel nor
     * the chat room subscription the read needs outlives the screen.
     *
     * @param chatIds Ids of the chats the action was picked for.
     * @param actionId Test-tag identity of the picked action.
     */
    fun onBulkChatRoomActionSelected(chatIds: Set<Long>, actionId: String) {
        val rooms = offeredActions?.roomsFor(chatIds, actionId) ?: run {
            Timber.w("Ignoring action $actionId, which was not offered for the selected chats")
            return
        }
        applicationScope.launch {
            chatRoomActions.run(actionId, rooms)
        }
    }

    /**
     * Publish [status] as the user's presence.
     *
     * @param status Presence the user picked.
     */
    fun onChatStatusSelected(status: UserChatStatus) {
        viewModelScope.launch {
            runCatchingCancellable { setCurrentUserStatusUseCase(status) }.onFailure { e ->
                Timber.e(e, "Failed to set the user status to $status")
                queueMessage(sharedR.string.changing_status_error)
            }
        }
    }

    /**
     * Mute options offered by the do-not-disturb toolbar action, which silences every chat at once.
     */
    fun doNotDisturbOptions(): List<ChatPushNotificationMuteOption> =
        runCatching { getChatMuteOptionListUseCase(emptyChatList = true) }
            .getOrElse { e ->
                Timber.e(e, "Failed to resolve the do not disturb options")
                emptyList()
            }

    /**
     * Apply the option the user picked from the do-not-disturb toolbar action to every chat: a
     * mute period, or [ChatPushNotificationMuteOption.Unmute] when they were already silenced.
     *
     * @param option Mute option the user picked.
     */
    fun onDoNotDisturbOptionSelected(option: ChatPushNotificationMuteOption) {
        viewModelScope.launch {
            runCatchingCancellable { muteChatPushNotificationUseCase(null, option) }.onFailure { e ->
                Timber.e(e, "Failed to apply $option to all chat notifications")
                queueMessage(sharedR.string.general_request_failed_message)
            }
        }
    }

    /**
     * Record that the tooltip currently owed has been seen, so [next] is offered instead.
     *
     * @param next Tooltip to show after the current one.
     */
    fun onMeetingTooltipShown(next: MeetingTooltipItem) {
        viewModelScope.launch {
            runCatching { setNextMeetingTooltipUseCase(next) }
                .onFailure { Timber.e(it, "Failed to store the next meeting tooltip") }
            tooltipChannel.send(Unit)
        }
    }

    private suspend fun queueMessage(messageId: Int) {
        runCatchingCancellable { snackbarEventQueue.queueMessage(messageId) }
            .onFailure { Timber.e(it, "Failed to queue a chat list message") }
    }

    private suspend fun selectedRooms(chatIds: Set<Long>): List<ChatRoomItem> {
        if (chatIds.isEmpty()) return emptyList()
        val rooms = currentChatRooms()
        return chatIds.mapNotNull { rooms[it] }
    }

    private suspend fun currentChatRooms(): Map<Long, ChatRoomItem> = runCatchingCancellable {
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

    private fun hasAnyContactFlow(): Flow<Boolean> = monitorHasAnyContactUseCase()
        .catch { e ->
            Timber.e(e, "Failed to monitor whether the user has any contact")
            emit(false)
        }

    private fun toolbarFlow(): Flow<Pair<ChatStatus?, Boolean>> =
        combine(chatStatusFlow(), doNotDisturbFlow(), ::Pair)

    private fun doNotDisturbFlow(): Flow<Boolean> = monitorChatsDoNotDisturbEnabledUseCase()
        .catch { e ->
            Timber.e(e, "Failed to monitor whether every chat is silenced")
            emit(false)
        }

    private fun chatStatusFlow(): Flow<ChatStatus?> = getCurrentChatStatusUseCase()
        .map<ChatStatus, ChatStatus?> { it }
        .onStart { emit(null) }
        .catch { e ->
            Timber.e(e, "Failed to monitor the user chat status")
            emit(null)
        }

    private fun meetingTooltipFlow(): Flow<MeetingTooltipItem> = tooltipChannel.receiveAsFlow()
        .onStart { emit(Unit) }
        .map { getMeetingTooltipsUseCase() }
        .catch { e ->
            Timber.e(e, "Failed to resolve the meeting tooltip")
            emit(MeetingTooltipItem.NONE)
        }

    private fun chatRoomsFlow(): Flow<ChatRooms> = combine(
        formattedChatRoomsFlow(ChatRoomType.NON_MEETINGS),
        formattedChatRoomsFlow(ChatRoomType.MEETINGS),
        hasArchivedChatsFlow(),
    ) { chats, meetings, hasArchived ->
        ChatRooms(
            chats = chats.map(chatRoomUiItemMapper::invoke).toImmutableList(),
            meetings = meetings.map(chatRoomUiItemMapper::invoke).toImmutableList(),
            hasArchived = hasArchived,
            isSearchable = isSearchable(chats, meetings),
        )
    }

    /**
     * There is nothing to search while both tabs are empty, or while the only chat the user has is
     * a note-to-self chat with no messages in it.
     */
    private fun isSearchable(
        chats: List<ChatRoomItem>,
        meetings: List<ChatRoomItem>,
    ): Boolean = when {
        chats.isEmpty() && meetings.isEmpty() -> false
        meetings.isNotEmpty() || chats.size > 1 -> true
        else -> chats.first().let {
            it !is ChatRoomItem.NoteToSelfChatRoomItem || !it.isEmptyNoteToSelfChatRoom
        }
    }

    /**
     * Only whether any archived chat exists is needed, so the archived list is asked for that one
     * answer on every chat list change instead of subscribing to the archived chat rooms pipeline,
     * which keeps resolving avatars, last messages and per-peer attributes for rows nothing renders.
     */
    private fun hasArchivedChatsFlow(): Flow<Boolean> = monitorChatListItemUpdates()
        .map { hasArchivedChatsUseCase() }
        .onStart { emit(hasArchivedChatsUseCase()) }
        .distinctUntilChanged()
        .catch { e ->
            Timber.e(e, "Failed to monitor whether the user has archived chats")
            emit(false)
        }

    private fun formattedChatRoomsFlow(
        chatRoomType: ChatRoomType,
    ): Flow<List<ChatRoomItem>> = getChatsUseCase(
        chatRoomType = chatRoomType,
        lastMessage = chatLastMessageFormatter::invoke,
        lastTimeMapper = chatRoomTimestampMapper::getLastTimeFormatted,
        meetingTimeMapper = chatRoomTimestampMapper::getMeetingTimeFormatted,
        headerTimeMapper = chatRoomTimestampMapper::getHeaderTimeFormatted,
    ).catch { e ->
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

    private fun List<ChatRoomUiItem>.anyUnread(): Boolean = any { it.unreadCount > 0 }


    /**
     * Chat rooms behind both tabs, independent of the active search query.
     *
     * @property chats Rows of the Chats tab.
     * @property meetings Rows of the Meetings tab.
     * @property hasArchived Whether the user has any archived chat.
     * @property isSearchable Whether there is anything worth searching.
     */
    private data class ChatRooms(
        val chats: ImmutableList<ChatRoomUiItem>,
        val meetings: ImmutableList<ChatRoomUiItem>,
        val hasArchived: Boolean,
        val isSearchable: Boolean,
    )
}
