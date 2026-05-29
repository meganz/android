package mega.privacy.android.app.presentation.chat.list.toolbar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.chat.list.model.ChatsTabState
import mega.privacy.android.app.presentation.chat.list.toolbar.ChatListSelectModeMenuAction.ArchiveAction
import mega.privacy.android.app.presentation.chat.list.toolbar.ChatListSelectModeMenuAction.LeaveAction
import mega.privacy.android.app.presentation.chat.list.toolbar.ChatListSelectModeMenuAction.MuteAction
import mega.privacy.android.app.presentation.chat.list.toolbar.ChatListSelectModeMenuAction.SelectAllAction
import mega.privacy.android.app.presentation.chat.list.toolbar.ChatListSelectModeMenuAction.UnmuteAction
import mega.privacy.android.app.presentation.chat.list.toolbar.ChatListSelectModeMenuAction.UnselectAllAction
import mega.privacy.android.app.presentation.extensions.text
import mega.privacy.android.app.presentation.meeting.model.NoteToSelfChatUIState
import mega.privacy.android.domain.entity.chat.ChatRoomItem
import mega.privacy.android.domain.entity.chat.ChatStatus
import mega.privacy.android.legacy.core.ui.controls.appbar.LegacySearchAppBar
import mega.privacy.android.legacy.core.ui.model.SearchWidgetState
import mega.privacy.android.shared.original.core.ui.controls.appbar.SelectModeAppBar
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

/**
 * Chat list toolbar component
 *
 * @param state Chat tabs state
 * @param noteToSelfChatState Note to self chat state
 * @param onNavigationClick Callback for navigation click
 * @param onChangeUserStatus Callback for user status change
 * @param onSearchTextChange Callback for search text change
 * @param onSearchCloseClicked Callback for search close
 * @param onOpenLinkActionClick Callback for open link action
 * @param onDoNotDisturbActionClick Callback for do not disturb action
 * @param onArchivedActionClick Callback for archived action
 */
@Composable
fun ChatListToolBar(
    state: ChatsTabState,
    noteToSelfChatState: NoteToSelfChatUIState,
    onNavigationClick: () -> Unit,
    onChangeUserStatus: () -> Unit,
    onSearchTextChange: (String) -> Unit,
    onSearchCloseClicked: () -> Unit,
    onOpenLinkActionClick: () -> Unit,
    onDoNotDisturbActionClick: () -> Unit,
    onArchivedActionClick: () -> Unit,
) {
    var searchWidgetState by remember { mutableStateOf(SearchWidgetState.COLLAPSED) }

    val showSearchButton = !state.areChatsOrMeetingLoading &&
            !state.isEmptyChatsOrMeetings &&
            (!state.onlyNoteToSelfChat || !noteToSelfChatState.isNoteToSelfChatEmpty)

    LegacySearchAppBar(
        modifier = Modifier.clickable(onClick = onChangeUserStatus),
        searchWidgetState = searchWidgetState,
        typedSearch = state.searchQuery ?: "",
        onSearchTextChange = onSearchTextChange,
        onCloseClicked = {
            searchWidgetState = SearchWidgetState.COLLAPSED
            onSearchCloseClicked()
        },
        onBackPressed = onNavigationClick,
        onSearchClicked = {
            searchWidgetState = SearchWidgetState.EXPANDED
        },
        subtitle = state.currentChatStatus?.text?.let { stringResource(it) },
        elevation = false,
        showSearchButton = showSearchButton,
        title = stringResource(R.string.section_chat),
        hintId = R.string.hint_action_search,
        windowInsets = WindowInsets(0.dp),
        actions = buildList {
            add(ChatListMenuAction.OpenLinkAction)
            add(ChatListMenuAction.DoNotDisturbAction)
            if (state.hasArchivedChats) {
                add(ChatListMenuAction.ArchivedAction)
            }
        },
        onActionPressed = { action ->
            when (action) {
                is ChatListMenuAction.OpenLinkAction -> onOpenLinkActionClick()
                is ChatListMenuAction.DoNotDisturbAction -> onDoNotDisturbActionClick()
                is ChatListMenuAction.ArchivedAction -> onArchivedActionClick()
            }
        }
    )
}

/**
 * Chat list toolbar shown while items are selected.
 *
 * Renders a [SelectModeAppBar] whose available actions depend on the current
 * selection (see [ChatListSelectModeMenuAction]).
 *
 * @param selectedItems Items currently selected in the active tab
 * @param currentItems All items in the active tab (used to compute "all selected")
 * @param onClearSelection Callback for clearing the current selection
 * @param onSelectAll Callback for selecting all items in the active tab
 * @param onMuteSelected Callback for muting all selected items
 * @param onUnmuteSelected Callback for unmuting all selected items
 * @param onArchiveSelected Callback for archiving all selected items
 * @param onLeaveSelected Callback for leaving all selected items
 */
@Composable
internal fun SelectionModeToolbar(
    selectedItems: List<ChatRoomItem>,
    currentItems: List<ChatRoomItem>,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onMuteSelected: () -> Unit,
    onUnmuteSelected: () -> Unit,
    onArchiveSelected: () -> Unit,
    onLeaveSelected: () -> Unit,
) {
    val actions: List<MenuAction> = remember(selectedItems, currentItems) {
        buildList {
            if (selectedItems.all { it.isMuteable && !it.isMuted }) {
                add(MuteAction)
            }
            if (selectedItems.all { it.isMuteable && it.isMuted }) {
                add(UnmuteAction)
            }
            add(ArchiveAction)
            if (selectedItems.all { it.isLeavable }) {
                add(LeaveAction)
            }
            if (selectedItems.size != currentItems.size) {
                add(SelectAllAction)
            }
            // Always available, matching the legacy selection ActionMode.
            add(UnselectAllAction)
        }
    }

    SelectModeAppBar(
        title = selectedItems.size.toString(),
        actions = actions,
        onNavigationPressed = onClearSelection,
        onActionPressed = { action ->
            when (action) {
                MuteAction -> onMuteSelected()
                UnmuteAction -> onUnmuteSelected()
                ArchiveAction -> onArchiveSelected()
                LeaveAction -> onLeaveSelected()
                SelectAllAction -> onSelectAll()
                UnselectAllAction -> onClearSelection()
            }
        },
    )
}

/**
 * Whether this chat can be muted/unmuted from the selection toolbar: it must be an
 * active chat and not the Note-to-Self chat.
 */
private val ChatRoomItem.isMuteable: Boolean
    get() = isActive && this !is ChatRoomItem.NoteToSelfChatRoomItem

/**
 * Whether this chat can be left from the selection toolbar: it must be an active
 * group chat or meeting.
 */
private val ChatRoomItem.isLeavable: Boolean
    get() = (this is ChatRoomItem.GroupChatRoomItem ||
            this is ChatRoomItem.MeetingChatRoomItem) && isActive

@PreviewLightDark
@Composable
fun ChatListToolBarPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        ChatListToolBar(
            state = ChatsTabState(
                currentChatStatus = ChatStatus.Online
            ),
            noteToSelfChatState = NoteToSelfChatUIState(),
            onNavigationClick = {},
            onChangeUserStatus = {},
            onSearchTextChange = {},
            onSearchCloseClicked = {},
            onOpenLinkActionClick = {},
            onDoNotDisturbActionClick = {},
            onArchivedActionClick = {}
        )
    }
}
