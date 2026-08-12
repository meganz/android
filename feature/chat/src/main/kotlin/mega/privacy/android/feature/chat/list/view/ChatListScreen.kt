package mega.privacy.android.feature.chat.list.view

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import mega.privacy.android.feature.chat.list.model.ChatListTabState
import mega.privacy.android.feature.chat.list.model.ChatListUiState
import mega.privacy.android.feature.chat.list.model.ChatRoomUiItem
import mega.privacy.android.shared.chats.components.ChatsViewSkeleton
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Chat list screen with Chats and Meetings tabs and in-tab search.
 *
 * Owns the search field state and delegates rendering to [ChatListScreenContent].
 *
 * @param uiState UI state of the screen.
 * @param showMeetingTab Whether the Meetings tab should be initially selected.
 * @param onBackClick Callback when back navigation is requested.
 * @param onItemClick Callback when a chat room row is clicked, with the chat id.
 * @param onSearchQueryChange Callback when the search query changes, with `null` when cleared.
 * @param modifier [Modifier]
 */
@Composable
internal fun ChatListScreen(
    uiState: ChatListUiState,
    showMeetingTab: Boolean,
    onBackClick: () -> Unit,
    onItemClick: (Long) -> Unit,
    onSearchQueryChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchMode by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(searchMode) {
        if (!searchMode && searchQuery.isNotEmpty()) {
            searchQuery = ""
            onSearchQueryChange(null)
        }
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
    )
}

/**
 * Stateless chat list screen rendering the search top app bar and the Chats and Meetings tabs.
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
) {
    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = modifier
            .fillMaxSize()
            .testTag(CHAT_LIST_SCREEN_TAG),
        topBar = {
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
                        )
                    }
                    addTextTabWithScrollableContent(
                        tabItem = TabItems(
                            title = stringResource(sharedR.string.chat_tab_meetings_title),
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
                        )
                    }
                },
            )
        }
    }
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
        scheduledTimestampFormatted = null,
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
    ),
    ChatRoomUiItem(
        chatId = 2L,
        title = "Recipe test #14",
        lastMessage = "Anna: Seeya all soon!",
        lastTimestampFormatted = "1 May 2022 17:53",
        scheduledTimestampFormatted = null,
        unreadCount = 0,
        isMuted = true,
        highlight = false,
        isNoteToSelf = false,
        avatar = ChatRoomUiItem.ChatRoomUiAvatar.Group,
        status = ContactItemStatus.Unknown,
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
