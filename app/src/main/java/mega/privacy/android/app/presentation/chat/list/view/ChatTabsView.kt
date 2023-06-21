package mega.privacy.android.app.presentation.chat.list.view

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.chat.list.model.ChatTab
import mega.privacy.android.app.presentation.chat.list.model.ChatsTabState
import mega.privacy.android.core.ui.theme.extensions.red_600_red_300
import mega.privacy.android.domain.entity.chat.ChatRoomItem

/**
 * Chat tabs view
 *
 * @param state             [ChatsTabState]
 * @param onTabSelected
 * @param onItemClick
 * @param onItemMoreClick
 * @param onItemSelected
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatTabsView(
    state: ChatsTabState,
    onTabSelected: (ChatTab) -> Unit = {},
    onItemClick: (Long) -> Unit = {},
    onItemMoreClick: (ChatRoomItem) -> Unit = {},
    onItemSelected: (Long) -> Unit = {},
) {
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState()
    var scrollToTop by remember { mutableStateOf(false) }
    var filteredChats by remember { mutableStateOf<List<ChatRoomItem>?>(listOf()) }
    var filteredMeetings by remember { mutableStateOf<List<ChatRoomItem>?>(listOf()) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            backgroundColor = MaterialTheme.colors.surface,
            contentColor = MaterialTheme.colors.red_600_red_300
        ) {
            ChatTab.values().forEachIndexed { index, item ->
                Tab(
                    text = { Text(text = stringResource(id = item.titleStringRes)) },
                    selected = pagerState.currentPage == index,
                    onClick = {
                        if (pagerState.currentPage != index) {
                            coroutineScope.launch { pagerState.animateScrollToPage(index) }
                        } else {
                            scrollToTop = !scrollToTop
                        }
                    }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            pageCount = ChatTab.values().size,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            Column {
                val items = if (page == ChatTab.CHATS.ordinal) {
                    filteredChats ?: state.chats
                } else {
                    filteredMeetings ?: state.meetings
                }

                ChatListView(
                    items = items,
                    selectedIds = state.selectedIds,
                    scrollToTop = scrollToTop,
                    onItemClick = onItemClick,
                    onItemMoreClick = onItemMoreClick,
                    onItemSelected = onItemSelected,
                )
            }
        }

        LaunchedEffect(state.searchQuery) {
            state.searchQuery?.takeIf(String::isNotBlank)?.let { searchQuery ->
                if (pagerState.currentPage == ChatTab.CHATS.ordinal) {
                    filteredChats = state.chats.filter { item ->
                        item.title.contains(searchQuery, true) ||
                                item.lastMessage?.contains(searchQuery, true) == true
                    }
                } else {
                    filteredMeetings = state.meetings.filter { item ->
                        item.title.contains(searchQuery, true) ||
                                item.lastMessage?.contains(searchQuery, true) == true
                    }
                }
            } ?: run {
                filteredChats = null
                filteredMeetings = null
            }
        }

        LaunchedEffect(pagerState.currentPage) {
            onTabSelected(ChatTab.values()[pagerState.currentPage])
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, name = "PreviewChatTabsView")
@Composable
private fun PreviewEmptyView() {
    ChatTabsView(
        state = ChatsTabState(),
    )
}
