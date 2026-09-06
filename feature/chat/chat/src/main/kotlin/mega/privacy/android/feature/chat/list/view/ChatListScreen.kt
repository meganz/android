package mega.privacy.android.feature.chat.list.view

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import kotlinx.collections.immutable.persistentListOf
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.android.core.ui.components.tabs.MegaCollapsibleTabRow
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaSearchTopAppBar
import mega.android.core.ui.model.TabItems
import mega.android.core.ui.modifiers.excludingBottomPadding
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.chat.list.model.ChatListBulkActionsUiState
import mega.privacy.android.feature.chat.list.model.ChatListTabState
import mega.privacy.android.feature.chat.list.model.ChatListUiState
import mega.privacy.android.feature.chat.list.model.ChatRoomActionUiItem
import mega.privacy.android.feature.chat.list.model.ChatRoomUiItem
import mega.privacy.android.feature.chat.list.model.rememberChatListSelectionState
import mega.privacy.android.shared.chats.components.ChatsViewSkeleton
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Chat list screen with Chats and Meetings tabs and in-tab search.
 *
 * Owns the search field state, the Compose multi-selection state, and the bulk-actions overlay,
 * and delegates rendering to [ChatListScreenContent].
 *
 * @param uiState UI state of the screen.
 * @param showMeetingTab Whether the Meetings tab should be initially selected.
 * @param onBackClick Callback when back navigation is requested.
 * @param onItemClick Callback when a chat room row is clicked, with the chat id.
 * @param onSearchQueryChange Callback when the search query changes, with `null` when cleared.
 * @param modifier [Modifier]
 * @param resolveBulkActions Resolves the ordered actions common to the selected chats.
 * @param onBulkActionSelected Runs the picked action across the selected chats.
 */
@Composable
internal fun ChatListScreen(
    uiState: ChatListUiState,
    showMeetingTab: Boolean,
    onBackClick: () -> Unit,
    onItemClick: (Long) -> Unit,
    onSearchQueryChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
    resolveBulkActions: suspend (Set<Long>) -> List<ChatRoomActionUiItem> = { emptyList() },
    onBulkActionSelected: (Set<Long>, String) -> Unit = { _, _ -> },
) {
    var searchMode by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val selectionState = rememberChatListSelectionState()
    var bulkActionsState by rememberSaveable(stateSaver = ChatListBulkActionsUiState.Saver) {
        mutableStateOf<ChatListBulkActionsUiState>(ChatListBulkActionsUiState.Hidden)
    }

    val selectionMode = selectionState.isInSelectionMode
    val selectableChatIds = remember(uiState) { uiState.selectableChatIds() }

    LaunchedEffect(searchMode) {
        if (!searchMode && searchQuery.isNotEmpty()) {
            searchQuery = ""
            onSearchQueryChange(null)
        }
    }

    LaunchedEffect(selectionMode) {
        if (!selectionMode) {
            bulkActionsState = ChatListBulkActionsUiState.Hidden
        }
    }

    BackHandler(enabled = selectionMode) {
        selectionState.deselectAll()
    }

    ChatListScreenContent(
        uiState = uiState,
        showMeetingTab = showMeetingTab,
        searchMode = searchMode,
        searchQuery = searchQuery,
        onBackClick = onBackClick,
        onItemClick = onItemClick,
        onSearchModeChange = { searchMode = it },
        onQueryChange = {
            searchQuery = it
            onSearchQueryChange(it.ifBlank { null })
        },
        modifier = modifier,
        onItemLongClick = selectionState::toggleSelection,
        selectionMode = selectionMode,
        selectedChatIds = selectionState.selectedChatIds,
        selectedCount = selectionState.selectedCount,
        isAllSelected = selectableChatIds.isNotEmpty() &&
                selectionState.selectedChatIds.containsAll(selectableChatIds),
        onSelectionToggle = selectionState::toggleSelection,
        onSelectAll = { selectionState.selectAll(selectableChatIds) },
        onCancelSelection = selectionState::deselectAll,
        onBulkActionsClick = {
            bulkActionsState = ChatListBulkActionsUiState.ShowingActions(selectionState.selectedChatIds)
        },
    )

    ChatListBulkActionsOverlay(
        actionsState = bulkActionsState,
        resolveActions = resolveBulkActions,
        onStateChange = { bulkActionsState = it },
        onActionSelected = { chatIds, actionId ->
            onBulkActionSelected(chatIds, actionId)
            selectionState.deselectAll()
        },
    )
}

@Composable
private fun ChatListBulkActionsOverlay(
    actionsState: ChatListBulkActionsUiState,
    resolveActions: suspend (Set<Long>) -> List<ChatRoomActionUiItem>,
    onStateChange: (ChatListBulkActionsUiState) -> Unit,
    onActionSelected: (Set<Long>, String) -> Unit,
) {
    when (actionsState) {
        ChatListBulkActionsUiState.Hidden -> Unit

        is ChatListBulkActionsUiState.ShowingActions -> {
            val chatIds = actionsState.chatIds
            var actions by remember(chatIds) {
                mutableStateOf<List<ChatRoomActionUiItem>?>(null)
            }
            LaunchedEffect(chatIds) {
                val resolved = resolveActions(chatIds)
                if (resolved.isEmpty()) {
                    onStateChange(ChatListBulkActionsUiState.Hidden)
                } else {
                    actions = resolved
                }
            }
            actions?.let { resolved ->
                ChatRoomActionsBottomSheet(
                    actions = resolved,
                    onDismiss = { onStateChange(ChatListBulkActionsUiState.Hidden) },
                    onActionClick = { action ->
                        val confirmation = action.confirmation
                        if (confirmation != null) {
                            onStateChange(
                                ChatListBulkActionsUiState.Confirming(
                                    chatIds = chatIds,
                                    actionId = action.testTag,
                                    confirmation = confirmation,
                                )
                            )
                        } else {
                            onActionSelected(chatIds, action.testTag)
                            onStateChange(ChatListBulkActionsUiState.Hidden)
                        }
                    },
                )
            }
        }

        is ChatListBulkActionsUiState.Confirming -> {
            val chatIds = actionsState.chatIds
            val actionId = actionsState.actionId
            ChatRoomActionConfirmationDialog(
                confirmation = actionsState.confirmation,
                onConfirm = {
                    onActionSelected(chatIds, actionId)
                    onStateChange(ChatListBulkActionsUiState.Hidden)
                },
                onDismiss = { onStateChange(ChatListBulkActionsUiState.Hidden) },
            )
        }
    }
}

/**
 * Stateless chat list screen rendering the search top app bar (or the selection top app bar while
 * in multi-select) and the Chats and Meetings tabs.
 *
 * @param uiState UI state of the screen.
 * @param showMeetingTab Whether the Meetings tab should be initially selected.
 * @param searchMode Whether the search field is active.
 * @param searchQuery Current search query text.
 * @param onBackClick Callback when back navigation is requested.
 * @param onItemClick Callback when a chat room row is clicked, with the chat id.
 * @param onSearchModeChange Callback when the search field is opened or closed.
 * @param onQueryChange Callback when the search query text changes.
 * @param modifier [Modifier]
 * @param onItemLongClick Optional callback when a chat room row is long-pressed, with the chat id.
 * @param selectionMode Whether the list is in multi-select mode.
 * @param selectedChatIds Ids of the currently selected chats.
 * @param selectedCount Number of selected chats, shown in the selection top app bar.
 * @param isAllSelected Whether every selectable chat is already selected.
 * @param onSelectionToggle Callback when a row's selection is toggled, with the chat id.
 * @param onSelectAll Callback when the select-all action is pressed.
 * @param onCancelSelection Callback when selection mode is dismissed.
 * @param onBulkActionsClick Callback when the bulk actions overflow is pressed.
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
    onSearchModeChange: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onItemLongClick: ((Long) -> Unit)? = null,
    selectionMode: Boolean = false,
    selectedChatIds: Set<Long> = emptySet(),
    selectedCount: Int = 0,
    isAllSelected: Boolean = false,
    onSelectionToggle: ((Long) -> Unit)? = null,
    onSelectAll: () -> Unit = {},
    onCancelSelection: () -> Unit = {},
    onBulkActionsClick: () -> Unit = {},
) {
    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = modifier
            .fillMaxSize()
            .testTag(CHAT_LIST_SCREEN_TAG),
        topBar = {
            if (selectionMode) {
                ChatListSelectionAppBar(
                    count = selectedCount,
                    isAllSelected = isAllSelected,
                    onSelectAllClick = onSelectAll,
                    onCancelClick = onCancelSelection,
                    onMoreClick = onBulkActionsClick,
                )
            } else {
                MegaSearchTopAppBar(
                    modifier = Modifier.testTag(CHAT_LIST_SEARCH_BAR_TAG),
                    title = stringResource(sharedR.string.general_chats_label),
                    navigationType = AppBarNavigationType.Back(onBackClick),
                    query = searchQuery,
                    isSearchingMode = searchMode,
                    onQueryChanged = onQueryChange,
                    onSearchingModeChanged = onSearchModeChange,
                    searchPlaceholder = stringResource(sharedR.string.search_placeholder_chat),
                )
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
                            onItemClick = onItemClick,
                            modifier = tabModifier,
                            contentPadding = PaddingValues(
                                bottom = paddingValues.calculateBottomPadding(),
                            ),
                            onItemLongClick = onItemLongClick,
                            selectionMode = selectionMode,
                            selectedChatIds = selectedChatIds,
                            onSelectionToggle = onSelectionToggle,
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
                            onItemClick = onItemClick,
                            modifier = tabModifier,
                            contentPadding = PaddingValues(
                                bottom = paddingValues.calculateBottomPadding(),
                            ),
                            onItemLongClick = onItemLongClick,
                            selectionMode = selectionMode,
                            selectedChatIds = selectedChatIds,
                            onSelectionToggle = onSelectionToggle,
                        )
                    }
                },
            )
        }
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

internal const val CHAT_LIST_SCREEN_TAG = "chat_list_screen:screen"
internal const val CHAT_LIST_SEARCH_BAR_TAG = "chat_list_screen:search_bar"
internal const val CHAT_LIST_LOADING_TAG = "chat_list_screen:loading"
internal const val CHAT_LIST_TAB_ROW_TAG = "chat_list_screen:tab_row"
internal const val CHAT_LIST_CHATS_TAB_TAG = "chat_list_screen:chats_tab"
internal const val CHAT_LIST_MEETINGS_TAB_TAG = "chat_list_screen:meetings_tab"

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

@CombinedThemePreviews
@Composable
private fun ChatListScreenPreview() {
    AndroidThemeForPreviews {
        ChatListScreen(
            uiState = ChatListUiState.Data(
                chats = ChatListTabState.Results(previewChats),
                meetings = ChatListTabState.Empty,
                chatsHaveUnread = true,
                meetingsHaveUnread = false,
            ),
            showMeetingTab = false,
            onBackClick = {},
            onItemClick = {},
            onSearchQueryChange = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ChatListScreenSearchActivePreview() {
    AndroidThemeForPreviews {
        ChatListScreenContent(
            uiState = ChatListUiState.Data(
                chats = ChatListTabState.Results(previewChats),
                meetings = ChatListTabState.Empty,
                chatsHaveUnread = true,
                meetingsHaveUnread = false,
            ),
            showMeetingTab = false,
            searchMode = true,
            searchQuery = "Re",
            onBackClick = {},
            onItemClick = {},
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
            uiState = ChatListUiState.Data(
                chats = ChatListTabState.NoSearchResults,
                meetings = ChatListTabState.NoSearchResults,
                chatsHaveUnread = false,
                meetingsHaveUnread = false,
            ),
            showMeetingTab = false,
            searchMode = true,
            searchQuery = "zzz",
            onBackClick = {},
            onItemClick = {},
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
            uiState = ChatListUiState.Data(
                chats = ChatListTabState.Results(previewChats),
                meetings = ChatListTabState.Empty,
                chatsHaveUnread = false,
                meetingsHaveUnread = false,
            ),
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
            onSearchQueryChange = {},
        )
    }
}
