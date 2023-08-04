package mega.privacy.android.app.presentation.chat.list.view

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.chat.list.model.ChatTab
import mega.privacy.android.app.presentation.chat.list.model.ChatsTabState
import mega.privacy.android.app.presentation.meeting.model.ScheduledMeetingManagementState
import mega.privacy.android.app.presentation.meeting.view.CancelScheduledMeetingDialog
import mega.privacy.android.core.ui.controls.tooltips.MegaTooltip
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_054_white_alpha_054
import mega.privacy.android.core.ui.theme.extensions.red_600_red_300
import mega.privacy.android.core.ui.theme.extensions.white_black
import mega.privacy.android.domain.entity.chat.ChatRoomItem
import mega.privacy.android.domain.entity.chat.MeetingTooltipItem

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
    managementState: ScheduledMeetingManagementState,
    onTabSelected: (ChatTab) -> Unit = {},
    onItemClick: (Long) -> Unit = {},
    onItemMoreClick: (ChatRoomItem) -> Unit = {},
    onItemSelected: (Long) -> Unit = {},
    onResetStateSnackbarMessage: () -> Unit = {},
    onResetManagementStateSnackbarMessage: () -> Unit = {},
    onCancelScheduledMeeting: () -> Unit = {},
    onDismissDialog: () -> Unit = {},
    onStartChatClick: () -> Unit = {},
    onShowNextTooltip: (MeetingTooltipItem) -> Unit = {},
) {
    val context = LocalContext.current
    val scaffoldState = rememberScaffoldState()
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState()
    var scrollToTop by remember { mutableStateOf(false) }
    var showFabButton by remember { mutableStateOf(true) }
    var filteredChats by remember { mutableStateOf<List<ChatRoomItem>?>(listOf()) }
    var filteredMeetings by remember { mutableStateOf<List<ChatRoomItem>?>(listOf()) }

    Scaffold(
        scaffoldState = scaffoldState,
        snackbarHost = { hostState ->
            SnackbarHost(
                hostState = hostState,
                snackbar = { data ->
                    Snackbar(
                        snackbarData = data,
                        modifier = Modifier.padding(bottom = 4.dp),
                        backgroundColor = MaterialTheme.colors.onPrimary,
                    )
                }
            )
        },
        floatingActionButton = {
            if (state.tooltip == MeetingTooltipItem.CREATE && pagerState.currentPage == ChatTab.MEETINGS.ordinal) {
                MegaTooltip(
                    titleText = stringResource(R.string.chat_schedule_meeting),
                    descriptionText = stringResource(R.string.meeting_list_tooltip_fab_description),
                    actionText = stringResource(R.string.button_permission_info),
                    showOnTop = true,
                    arrowPosition = 0.89f,
                    onDismissed = { onShowNextTooltip(MeetingTooltipItem.RECURRING_OR_PENDING) },
                ) {
                    FabButton(true, onStartChatClick)
                }
            } else {
                FabButton(showFabButton, onStartChatClick)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                backgroundColor = MaterialTheme.colors.surface,
                contentColor = MaterialTheme.colors.red_600_red_300
            ) {
                ChatTab.values().forEachIndexed { index, item ->
                    Tab(text = { Text(text = stringResource(item.titleStringRes)) },
                        selected = pagerState.currentPage == index,
                        unselectedContentColor = MaterialTheme.colors.grey_alpha_054_white_alpha_054,
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
                val isMeetingView = page == ChatTab.MEETINGS.ordinal
                val items = if (isMeetingView) {
                    filteredMeetings ?: state.meetings
                } else {
                    filteredChats ?: state.chats
                }

                ChatListView(
                    items = items,
                    selectedIds = state.selectedIds,
                    scrollToTop = scrollToTop,
                    onItemClick = onItemClick,
                    isMeetingView = isMeetingView,
                    tooltip = state.tooltip,
                    onItemMoreClick = onItemMoreClick,
                    onItemSelected = onItemSelected,
                    onScrollInProgress = { showFabButton = !it },
                    onEmptyButtonClick = onStartChatClick,
                    onShowNextTooltip = onShowNextTooltip,
                )
            }

            LaunchedEffect(state.searchQuery) {
                state.searchQuery?.takeIf(String::isNotBlank)?.let { searchQuery ->
                    if (pagerState.currentPage == ChatTab.CHATS.ordinal) {
                        filteredChats = state.chats.filter { item ->
                            item.title.contains(searchQuery, true) || item.lastMessage?.contains(
                                searchQuery,
                                true
                            ) == true
                        }
                    } else {
                        filteredMeetings = state.meetings.filter { item ->
                            item.title.contains(searchQuery, true) || item.lastMessage?.contains(
                                searchQuery,
                                true
                            ) == true
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

            EventEffect(
                event = state.snackbarMessageContent, onConsumed = onResetStateSnackbarMessage
            ) { resId ->
                scaffoldState.snackbarHostState.showSnackbar(context.resources.getString(resId))
            }

            EventEffect(
                event = managementState.snackbarMessageContent,
                onConsumed = onResetManagementStateSnackbarMessage
            ) {
                scaffoldState.snackbarHostState.showSnackbar(it)
            }
        }
    }

    managementState.isChatHistoryEmpty?.let { isChatHistoryEmpty ->
        managementState.chatRoomItem?.let { chatRoomItem ->
            managementState.chatRoom?.let { chatRoom ->
                CancelScheduledMeetingDialog(
                    isChatHistoryEmpty = isChatHistoryEmpty,
                    isRecurringMeeting = chatRoomItem.isRecurringMeeting(),
                    chatTitle = chatRoom.title,
                    onConfirm = onCancelScheduledMeeting,
                    onDismiss = onDismissDialog,
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalAnimationApi::class)
private fun FabButton(showFabButton: Boolean, onStartChatClick: () -> Unit) {
    AnimatedVisibility(
        visible = showFabButton,
        enter = scaleIn(),
        exit = scaleOut(),
    ) {
        FloatingActionButton(onClick = onStartChatClick) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Create new chat",
                tint = MaterialTheme.colors.white_black
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, name = "PreviewChatTabsView")
@Composable
private fun PreviewEmptyView() {
    ChatTabsView(
        state = ChatsTabState(),
        managementState = ScheduledMeetingManagementState(),
    )
}
