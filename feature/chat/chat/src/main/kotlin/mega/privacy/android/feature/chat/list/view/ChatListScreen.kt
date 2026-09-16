package mega.privacy.android.feature.chat.list.view

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import kotlinx.collections.immutable.persistentListOf
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.android.core.ui.components.fab.MegaFab
import mega.android.core.ui.components.tabs.MegaCollapsibleTabRow
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaSearchTopAppBar
import mega.android.core.ui.components.tooltip.direction.TooltipDirection
import mega.android.core.ui.components.tooltip.popup.interactive.InteractiveBottomDirectionTooltipPopup
import mega.android.core.ui.components.tooltip.popup.interactive.InteractiveTooltipButtonProperties
import mega.android.core.ui.model.TabItems
import mega.android.core.ui.modifiers.applyScrollToHideFabBehavior
import mega.android.core.ui.modifiers.excludingBottomPadding
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.chat.ChatPushNotificationMuteOption
import mega.privacy.android.domain.entity.chat.ChatStatus
import mega.privacy.android.domain.entity.chat.MeetingTooltipItem
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.feature.chat.list.model.ChatListBulkActionsUiState
import mega.privacy.android.feature.chat.list.model.ChatListTabState
import mega.privacy.android.feature.chat.list.model.ChatListUiState
import mega.privacy.android.feature.chat.list.model.ChatRoomActionUiItem
import mega.privacy.android.feature.chat.list.model.ChatRoomUiItem
import mega.privacy.android.feature.chat.list.model.MeetingAction
import mega.privacy.android.feature.chat.list.model.rememberChatListSelectionState
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.navigation.contract.state.isMainNavBarRoot
import mega.privacy.android.navigation.destination.ChatListNavKey
import mega.privacy.android.shared.chats.components.ChatsViewSkeleton
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Chat list screen with Chats and Meetings tabs and in-tab search.
 *
 * Owns the search field state, the Compose multi-selection state, the chat room actions overlay
 * and the presence and do-not-disturb dialogs, and delegates rendering to [ChatListScreenContent].
 *
 * @param uiState UI state of the screen.
 * @param showMeetingTab Whether the Meetings tab should be initially selected.
 * @param onBackClick Callback when back navigation is requested.
 * @param onItemClick Callback when a chat room row is clicked, with the chat id.
 * @param onNewChatClick Callback when the new-chat floating action button is clicked.
 * @param onInviteFriendClick Callback when the empty Chats tab invite-a-friend action is clicked.
 * @param onMeetingActionClick Callback when a meeting action is picked from the meetings sheet or
 * from the empty Meetings tab.
 * @param onArchivedClick Callback when the archived chats action is clicked.
 * @param onOpenLinkClick Callback when the open-link action is picked from the overflow.
 * @param onSearchQueryChange Callback when the search query changes, with `null` when cleared.
 * @param modifier [Modifier]
 * @param resolveChatRoomActions Resolves the ordered actions common to the given chats.
 * @param onChatRoomActionSelected Runs the picked action across the given chats.
 * @param onChatStatusSelected Callback with the presence the user picked.
 * @param doNotDisturbOptions Mute periods offered by the do-not-disturb action.
 * @param onDoNotDisturbOptionSelected Callback with the mute period the user picked.
 * @param onMeetingTooltipShown Callback with the tooltip to offer after the current one was seen.
 */
@Composable
internal fun ChatListScreen(
    uiState: ChatListUiState,
    showMeetingTab: Boolean,
    onBackClick: () -> Unit,
    onItemClick: (Long) -> Unit,
    onNewChatClick: () -> Unit,
    onInviteFriendClick: () -> Unit,
    onMeetingActionClick: (MeetingAction) -> Unit,
    onArchivedClick: () -> Unit,
    onOpenLinkClick: () -> Unit,
    onSearchQueryChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
    resolveChatRoomActions: suspend (Set<Long>) -> List<ChatRoomActionUiItem> = { emptyList() },
    onChatRoomActionSelected: (Set<Long>, String) -> Unit = { _, _ -> },
    onChatStatusSelected: (UserChatStatus) -> Unit = {},
    doNotDisturbOptions: () -> List<ChatPushNotificationMuteOption> = { emptyList() },
    onDoNotDisturbOptionSelected: (ChatPushNotificationMuteOption) -> Unit = {},
    onMeetingTooltipShown: (MeetingTooltipItem) -> Unit = {},
) {
    var searchMode by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val selectionState = rememberChatListSelectionState()
    var actionsState by rememberSaveable(stateSaver = ChatListBulkActionsUiState.Saver) {
        mutableStateOf<ChatListBulkActionsUiState>(ChatListBulkActionsUiState.Hidden)
    }
    var showMeetingActions by rememberSaveable { mutableStateOf(false) }
    var showStatusDialog by rememberSaveable { mutableStateOf(false) }
    var showDoNotDisturbDialog by rememberSaveable { mutableStateOf(false) }

    val selectionMode = selectionState.isInSelectionMode
    val selectableChatIds = remember(uiState) { uiState.selectableChatIds() }

    LaunchedEffect(searchMode) {
        if (!searchMode && searchQuery.isNotEmpty()) {
            searchQuery = ""
            onSearchQueryChange(null)
        }
    }

    val exitSelection = {
        selectionState.deselectAll()
        actionsState = ChatListBulkActionsUiState.Hidden
    }

    BackHandler(enabled = selectionMode || searchMode) {
        if (selectionMode) {
            exitSelection()
        } else {
            searchMode = false
        }
    }

    ChatListScreenContent(
        uiState = uiState,
        showMeetingTab = showMeetingTab,
        searchMode = searchMode,
        searchQuery = searchQuery,
        onBackClick = onBackClick,
        onItemClick = onItemClick,
        onNewChatClick = onNewChatClick,
        onInviteFriendClick = onInviteFriendClick,
        onMeetingFabClick = { showMeetingActions = true },
        onMeetingActionClick = onMeetingActionClick,
        onArchivedClick = onArchivedClick,
        onOpenLinkClick = onOpenLinkClick,
        onSearchModeChange = { searchMode = it },
        onQueryChange = {
            searchQuery = it
            onSearchQueryChange(it.ifBlank { null })
        },
        modifier = modifier,
        onItemLongClick = selectionState::toggleSelection,
        onItemMoreClick = { chatId ->
            actionsState = ChatListBulkActionsUiState.ShowingActions(setOf(chatId))
        },
        selectionMode = selectionMode,
        selectedChatIds = selectionState.selectedChatIds,
        selectedCount = selectionState.selectedCount,
        isAllSelected = selectableChatIds.isNotEmpty() &&
                selectionState.selectedChatIds.containsAll(selectableChatIds),
        onSelectionToggle = selectionState::toggleSelection,
        onSelectAll = { selectionState.selectAll(selectableChatIds) },
        onCancelSelection = exitSelection,
        onBulkActionsClick = {
            actionsState = ChatListBulkActionsUiState.ShowingActions(selectionState.selectedChatIds)
        },
        onPresenceClick = { showStatusDialog = true },
        onDoNotDisturbClick = { showDoNotDisturbDialog = true },
        onMeetingTooltipShown = onMeetingTooltipShown,
    )

    if (showMeetingActions) {
        MeetingActionsBottomSheet(
            onActionClick = onMeetingActionClick,
            onDismiss = { showMeetingActions = false },
        )
    }

    if (showStatusDialog) {
        ChatStatusDialog(
            chatStatus = (uiState as? ChatListUiState.Data)?.chatStatus,
            onStatusSelected = onChatStatusSelected,
            onDismiss = { showStatusDialog = false },
        )
    }

    if (showDoNotDisturbDialog) {
        val options = remember { doNotDisturbOptions() }
        ChatDoNotDisturbDialog(
            options = options,
            onOptionSelected = onDoNotDisturbOptionSelected,
            onDismiss = { showDoNotDisturbDialog = false },
        )
    }

    ChatRoomActionsOverlay(
        actionsState = actionsState,
        resolveActions = resolveChatRoomActions,
        onStateChange = { actionsState = it },
        onActionSelected = { chatIds, actionId ->
            onChatRoomActionSelected(chatIds, actionId)
            selectionState.deselectAll()
        },
    )
}

/**
 * Stateless chat list screen rendering the chat list top app bar, the search top app bar or the
 * selection top app bar, and the Chats and Meetings tabs.
 *
 * @param uiState UI state of the screen.
 * @param showMeetingTab Whether the Meetings tab should be initially selected.
 * @param searchMode Whether the search field is active.
 * @param searchQuery Current search query text.
 * @param onBackClick Callback when back navigation is requested.
 * @param onItemClick Callback when a chat room row is clicked, with the chat id.
 * @param onNewChatClick Callback when the floating action button is clicked on the Chats tab.
 * @param onInviteFriendClick Callback when the empty Chats tab invite-a-friend action is clicked.
 * @param onMeetingFabClick Callback when the floating action button is clicked on the Meetings tab.
 * @param onMeetingActionClick Callback when an action on the empty Meetings tab is clicked.
 * @param onArchivedClick Callback when the archived chats action is clicked.
 * @param onOpenLinkClick Callback when the open-link action is picked from the overflow.
 * @param onSearchModeChange Callback when the search field is opened or closed.
 * @param onQueryChange Callback when the search query text changes.
 * @param modifier [Modifier]
 * @param onItemLongClick Optional callback when a chat room row is long-pressed, with the chat id.
 * @param onItemMoreClick Optional callback when a row's actions affordance is pressed, with the
 * chat id.
 * @param selectionMode Whether the list is in multi-select mode.
 * @param selectedChatIds Ids of the currently selected chats.
 * @param selectedCount Number of selected chats, shown in the selection top app bar.
 * @param isAllSelected Whether every selectable chat is already selected.
 * @param onSelectionToggle Callback when a row's selection is toggled, with the chat id.
 * @param onSelectAll Callback when the select-all action is pressed.
 * @param onCancelSelection Callback when selection mode is dismissed.
 * @param onBulkActionsClick Callback when the bulk actions overflow is pressed.
 * @param onPresenceClick Callback when the user asks to change their presence.
 * @param onDoNotDisturbClick Callback when the do-not-disturb action is picked.
 * @param onMeetingTooltipShown Callback with the tooltip to offer after the current one was seen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatListScreenContent(
    uiState: ChatListUiState,
    showMeetingTab: Boolean,
    searchMode: Boolean,
    searchQuery: String,
    onBackClick: () -> Unit,
    onItemClick: (Long) -> Unit,
    onNewChatClick: () -> Unit,
    onInviteFriendClick: () -> Unit,
    onMeetingFabClick: () -> Unit,
    onMeetingActionClick: (MeetingAction) -> Unit,
    onArchivedClick: () -> Unit,
    onOpenLinkClick: () -> Unit,
    onSearchModeChange: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onItemLongClick: ((Long) -> Unit)? = null,
    onItemMoreClick: ((Long) -> Unit)? = null,
    selectionMode: Boolean = false,
    selectedChatIds: Set<Long> = emptySet(),
    selectedCount: Int = 0,
    isAllSelected: Boolean = false,
    onSelectionToggle: ((Long) -> Unit)? = null,
    onSelectAll: () -> Unit = {},
    onCancelSelection: () -> Unit = {},
    onBulkActionsClick: () -> Unit = {},
    onPresenceClick: () -> Unit = {},
    onDoNotDisturbClick: () -> Unit = {},
    onMeetingTooltipShown: (MeetingTooltipItem) -> Unit = {},
) {
    var selectedTabIndex by rememberSaveable {
        mutableIntStateOf(if (showMeetingTab) MEETINGS_TAB_INDEX else CHATS_TAB_INDEX)
    }
    var chatsScrollToTopToken by rememberSaveable { mutableIntStateOf(0) }
    var meetingsScrollToTopToken by rememberSaveable { mutableIntStateOf(0) }
    var meetingFabCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val navigationType = if (isMainNavBarRoot(ChatListNavKey::class)) {
        AppBarNavigationType.None
    } else {
        AppBarNavigationType.Back(onBackClick)
    }

    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = modifier
            .fillMaxSize()
            .testTag(CHAT_LIST_SCREEN_TAG),
        topBar = {
            when {
                selectionMode -> ChatListSelectionAppBar(
                    count = selectedCount,
                    isAllSelected = isAllSelected,
                    onSelectAllClick = onSelectAll,
                    onCancelClick = onCancelSelection,
                    onMoreClick = onBulkActionsClick,
                )

                searchMode -> MegaSearchTopAppBar(
                    modifier = Modifier.testTag(CHAT_LIST_SEARCH_BAR_TAG),
                    title = stringResource(sharedR.string.general_chats_label),
                    navigationType = navigationType,
                    query = searchQuery,
                    isSearchingMode = true,
                    onQueryChanged = onQueryChange,
                    onSearchingModeChanged = onSearchModeChange,
                    searchPlaceholder = stringResource(sharedR.string.search_placeholder_chat),
                )

                else -> ChatListTopAppBar(
                    chatStatus = (uiState as? ChatListUiState.Data)?.chatStatus,
                    isSearchAvailable = (uiState as? ChatListUiState.Data)?.isSearchAvailable
                        ?: false,
                    hasArchivedChats = (uiState as? ChatListUiState.Data)?.hasArchivedChats
                        ?: false,
                    navigationType = navigationType,
                    onPresenceClick = onPresenceClick,
                    onSearchClick = { onSearchModeChange(true) },
                    onArchivedClick = onArchivedClick,
                    onOpenLinkClick = onOpenLinkClick,
                    onDoNotDisturbClick = onDoNotDisturbClick,
                )
            }
        },
        floatingActionButton = {
            if (uiState is ChatListUiState.Data && !searchMode) {
                val isMeetingsTab = selectedTabIndex == MEETINGS_TAB_INDEX
                MegaFab(
                    modifier = Modifier
                        .testTag(if (isMeetingsTab) CHAT_LIST_MEETING_FAB_TAG else CHAT_LIST_FAB_TAG)
                        .onGloballyPositioned { meetingFabCoordinates = it }
                        .applyScrollToHideFabBehavior(),
                    onClick = if (isMeetingsTab) onMeetingFabClick else onNewChatClick,
                    painter = rememberVectorPainter(
                        if (isMeetingsTab) {
                            IconPack.Medium.Thin.Outline.VideoPlus
                        } else {
                            IconPack.Medium.Thin.Outline.Plus
                        }
                    ),
                    contentDescription = stringResource(
                        if (isMeetingsTab) {
                            sharedR.string.chat_meeting_actions_start_now
                        } else {
                            sharedR.string.new_chat_title
                        }
                    ),
                )
                if (isMeetingsTab && uiState.meetingTooltip == MeetingTooltipItem.CREATE) {
                    CreateMeetingTooltip(
                        anchorCoordinates = meetingFabCoordinates,
                        onDismiss = {
                            onMeetingTooltipShown(MeetingTooltipItem.RECURRING_OR_PENDING)
                        },
                    )
                }
            }
        },
    ) { paddingValues ->
        when (uiState) {
            ChatListUiState.Loading -> ChatsViewSkeleton(
                modifier = Modifier
                    .padding(paddingValues)
                    .testTag(CHAT_LIST_LOADING_TAG),
            )

            is ChatListUiState.Data -> MegaCollapsibleTabRow(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(CHAT_LIST_TAB_ROW_TAG),
                contentPadding = paddingValues.excludingBottomPadding(),
                initialSelectedIndex = if (showMeetingTab) MEETINGS_TAB_INDEX else CHATS_TAB_INDEX,
                onTabSelected = { index ->
                    if (index == selectedTabIndex) {
                        if (index == MEETINGS_TAB_INDEX) {
                            meetingsScrollToTopToken++
                        } else {
                            chatsScrollToTopToken++
                        }
                        false
                    } else {
                        selectedTabIndex = index
                        true
                    }
                },
                cells = {
                    addTextTabWithScrollableContent(
                        tabItem = TabItems(
                            title = stringResource(sharedR.string.general_chats_label),
                            showBadge = uiState.chatsHaveUnread,
                            testTag = CHAT_LIST_CHATS_TAB_TAG,
                        ),
                    ) { _, tabModifier ->
                        ChatListContent(
                            state = uiState.chats,
                            isMeetingsTab = false,
                            hasAnyContact = uiState.hasAnyContact,
                            onItemClick = onItemClick,
                            onNewChatClick = onNewChatClick,
                            onInviteFriendClick = onInviteFriendClick,
                            onMeetingActionClick = onMeetingActionClick,
                            modifier = tabModifier,
                            contentPadding = PaddingValues(
                                bottom = paddingValues.calculateBottomPadding() + FAB_BOTTOM_CLEARANCE,
                            ),
                            onItemLongClick = onItemLongClick,
                            onItemMoreClick = onItemMoreClick,
                            selectionMode = selectionMode,
                            selectedChatIds = selectedChatIds,
                            onSelectionToggle = onSelectionToggle,
                            scrollToTopToken = chatsScrollToTopToken,
                        )
                    }
                    addTextTabWithScrollableContent(
                        tabItem = TabItems(
                            title = stringResource(sharedR.string.chat_tab_meetings_title),
                            showBadge = uiState.meetingsHaveUnread,
                            testTag = CHAT_LIST_MEETINGS_TAB_TAG,
                        ),
                    ) { _, tabModifier ->
                        ChatListContent(
                            state = uiState.meetings,
                            isMeetingsTab = true,
                            hasAnyContact = uiState.hasAnyContact,
                            onItemClick = onItemClick,
                            onNewChatClick = onNewChatClick,
                            onInviteFriendClick = onInviteFriendClick,
                            onMeetingActionClick = onMeetingActionClick,
                            modifier = tabModifier,
                            contentPadding = PaddingValues(
                                bottom = paddingValues.calculateBottomPadding() + FAB_BOTTOM_CLEARANCE,
                            ),
                            onItemLongClick = onItemLongClick,
                            onItemMoreClick = onItemMoreClick,
                            selectionMode = selectionMode,
                            selectedChatIds = selectedChatIds,
                            onSelectionToggle = onSelectionToggle,
                            scrollToTopToken = meetingsScrollToTopToken,
                        )
                    }
                },
            )
        }
    }
}

/**
 * One-time onboarding tooltip pointing at the meetings floating action button.
 *
 * @param anchorCoordinates Coordinates of the floating action button, or null until it has been
 * laid out; the tooltip then waits.
 * @param onDismiss Called when the tooltip is dismissed.
 */
@Composable
private fun CreateMeetingTooltip(
    anchorCoordinates: LayoutCoordinates?,
    onDismiss: () -> Unit,
) {
    anchorCoordinates?.takeIf { it.isAttached }?.let { coordinates ->
        InteractiveBottomDirectionTooltipPopup(
            modifier = Modifier.testTag(CHAT_LIST_CREATE_MEETING_TOOLTIP_TAG),
            direction = TooltipDirection.Bottom.Right,
            properties = PopupProperties(
                focusable = false,
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
            ),
            title = stringResource(sharedR.string.chat_meeting_actions_schedule),
            body = stringResource(sharedR.string.meeting_list_tooltip_fab_description),
            primaryButton = InteractiveTooltipButtonProperties(
                text = stringResource(sharedR.string.general_ok),
                onClick = onDismiss,
            ),
            needCloseIcon = true,
            needDivider = true,
            anchorViewCoordinates = coordinates,
            onDismissRequest = onDismiss,
        )
    }
}

private fun ChatListUiState.selectableChatIds(): Set<Long> = when (this) {
    ChatListUiState.Loading -> emptySet()
    is ChatListUiState.Data -> (chats.chatIds() + meetings.chatIds()).toSet()
}

private fun ChatListTabState.chatIds(): List<Long> = when (this) {
    is ChatListTabState.Results -> items.map(ChatRoomUiItem::chatId)
    ChatListTabState.Empty, ChatListTabState.NoSearchResults -> emptyList()
}

private const val CHATS_TAB_INDEX = 0
private const val MEETINGS_TAB_INDEX = 1

private val FAB_BOTTOM_CLEARANCE = 88.dp

internal const val CHAT_LIST_SCREEN_TAG = "chat_list_screen:screen"
internal const val CHAT_LIST_SEARCH_BAR_TAG = "chat_list_screen:search_bar"
internal const val CHAT_LIST_LOADING_TAG = "chat_list_screen:loading"
internal const val CHAT_LIST_TAB_ROW_TAG = "chat_list_screen:tab_row"
internal const val CHAT_LIST_CHATS_TAB_TAG = "chat_list_screen:chats_tab"
internal const val CHAT_LIST_MEETINGS_TAB_TAG = "chat_list_screen:meetings_tab"
internal const val CHAT_LIST_FAB_TAG = "chat_list_screen:new_chat_fab"
internal const val CHAT_LIST_MEETING_FAB_TAG = "chat_list_screen:new_meeting_fab"
internal const val CHAT_LIST_CREATE_MEETING_TOOLTIP_TAG = "chat_list_screen:create_meeting_tooltip"

private val previewChats = persistentListOf(
    ChatRoomUiItem(
        chatId = 1L,
        title = "Mieko Kawakami",
        lastMessage = "See you tomorrow!",
        lastTimestampFormatted = "Today 14:25",
        header = null,
        pendingMeeting = null,
        showRecurringIcon = false,
        unreadCount = 5,
        isMuted = false,
        highlight = true,
        isNoteToSelf = false,
        avatar = ChatRoomUiItem.ChatRoomUiAvatar.Peer(
            placeholderText = "M",
            filePath = null,
            color = null,
        ),
        status = ContactItemStatus.Online,
        hasOngoingCall = false,
    ),
    ChatRoomUiItem(
        chatId = 2L,
        title = "Recipe test #14",
        lastMessage = "Anna: Seeya all soon!",
        lastTimestampFormatted = "1 May 2022 17:53",
        header = null,
        pendingMeeting = null,
        showRecurringIcon = false,
        unreadCount = 0,
        isMuted = true,
        highlight = false,
        isNoteToSelf = false,
        avatar = ChatRoomUiItem.ChatRoomUiAvatar.Group,
        status = ContactItemStatus.Unknown,
        hasOngoingCall = false,
    ),
)

private fun previewState(
    chats: ChatListTabState = ChatListTabState.Results(previewChats),
    meetings: ChatListTabState = ChatListTabState.Empty,
    hasAnyContact: Boolean = true,
    hasArchivedChats: Boolean = true,
    isSearchAvailable: Boolean = true,
) = ChatListUiState.Data(
    chats = chats,
    meetings = meetings,
    chatsHaveUnread = true,
    meetingsHaveUnread = false,
    hasAnyContact = hasAnyContact,
    hasArchivedChats = hasArchivedChats,
    isSearchAvailable = isSearchAvailable,
    chatStatus = ChatStatus.Online,
    meetingTooltip = MeetingTooltipItem.NONE,
)

@CombinedThemePreviews
@Composable
private fun ChatListScreenPreview() {
    AndroidThemeForPreviews {
        ChatListScreen(
            uiState = previewState(),
            showMeetingTab = false,
            onBackClick = {},
            onItemClick = {},
            onNewChatClick = {},
            onInviteFriendClick = {},
            onMeetingActionClick = {},
            onArchivedClick = {},
            onOpenLinkClick = {},
            onSearchQueryChange = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ChatListScreenSearchActivePreview() {
    AndroidThemeForPreviews {
        ChatListScreenContent(
            uiState = previewState(),
            showMeetingTab = false,
            searchMode = true,
            searchQuery = "Re",
            onBackClick = {},
            onItemClick = {},
            onNewChatClick = {},
            onInviteFriendClick = {},
            onMeetingFabClick = {},
            onMeetingActionClick = {},
            onArchivedClick = {},
            onOpenLinkClick = {},
            onSearchModeChange = {},
            onQueryChange = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ChatListScreenNoResultsPreview() {
    AndroidThemeForPreviews {
        ChatListScreenContent(
            uiState = previewState(
                chats = ChatListTabState.NoSearchResults,
                meetings = ChatListTabState.NoSearchResults,
            ),
            showMeetingTab = false,
            searchMode = true,
            searchQuery = "zzz",
            onBackClick = {},
            onItemClick = {},
            onNewChatClick = {},
            onInviteFriendClick = {},
            onMeetingFabClick = {},
            onMeetingActionClick = {},
            onArchivedClick = {},
            onOpenLinkClick = {},
            onSearchModeChange = {},
            onQueryChange = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ChatListScreenSelectionModePreview() {
    AndroidThemeForPreviews {
        ChatListScreenContent(
            uiState = previewState(),
            showMeetingTab = false,
            searchMode = false,
            searchQuery = "",
            onBackClick = {},
            onItemClick = {},
            onSearchModeChange = {},
            onQueryChange = {},
            selectionMode = true,
            selectedChatIds = setOf(1L),
            selectedCount = 1,
            isAllSelected = false,
            onNewChatClick = {},
            onInviteFriendClick = {},
            onMeetingFabClick = {},
            onMeetingActionClick = {},
            onArchivedClick = {},
            onOpenLinkClick = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ChatListScreenLoadingPreview() {
    AndroidThemeForPreviews {
        ChatListScreen(
            uiState = ChatListUiState.Loading,
            showMeetingTab = false,
            onBackClick = {},
            onItemClick = {},
            onNewChatClick = {},
            onInviteFriendClick = {},
            onMeetingActionClick = {},
            onArchivedClick = {},
            onOpenLinkClick = {},
            onSearchQueryChange = {},
        )
    }
}
