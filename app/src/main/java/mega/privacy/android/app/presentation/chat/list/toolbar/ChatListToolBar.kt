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
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.chat.list.model.ChatsTabState
import mega.privacy.android.app.presentation.extensions.text
import mega.privacy.android.app.presentation.meeting.model.NoteToSelfChatUIState
import mega.privacy.android.domain.entity.chat.ChatStatus
import mega.privacy.android.legacy.core.ui.controls.appbar.LegacySearchAppBar
import mega.privacy.android.legacy.core.ui.model.SearchWidgetState
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

