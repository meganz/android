package mega.privacy.android.app.presentation.chat.list.view

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.palm.composestateevents.EventEffect
import mega.privacy.android.app.R
import mega.privacy.android.app.extensions.normalize
import mega.privacy.android.app.presentation.chat.list.model.ChatTab
import mega.privacy.android.app.presentation.chat.list.model.ChatsTabState
import mega.privacy.android.app.presentation.meeting.model.ScheduledMeetingManagementUiState
import mega.privacy.android.app.presentation.meeting.view.dialog.CancelScheduledMeetingDialog
import mega.privacy.android.app.presentation.meeting.view.dialog.ForceAppUpdateDialog
import mega.privacy.android.domain.entity.chat.ChatRoomItem
import mega.privacy.android.domain.entity.chat.MeetingTooltipItem
import mega.privacy.android.legacy.core.ui.controls.tooltips.LegacyMegaTooltip
import mega.privacy.android.shared.original.core.ui.controls.tab.Tabs
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.white_black
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar

/**
 * Chat tabs view
 *
 * @param state             [ChatsTabState]
 * @param managementState   [ScheduledMeetingManagementUiState]
 * @param showMeetingTab    True to show Meeting tab as initial tab or false (default) otherwise
 * @param onTabSelected
 * @param onItemClick
 * @param onItemMoreClick
 * @param onItemSelected
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatTabsView(
    state: ChatsTabState,
    managementState: ScheduledMeetingManagementUiState,
    showMeetingTab: Boolean = false,
    onTabSelected: (ChatTab) -> Unit = {},
    onItemClick: (Long) -> Unit = {},
    onItemMoreClick: (ChatRoomItem) -> Unit = {},
    onItemSelected: (Long) -> Unit = {},
    onResetStateSnackbarMessage: () -> Unit = {},
    onResetManagementStateSnackbarMessage: () -> Unit = {},
    onCancelScheduledMeeting: () -> Unit = {},
    onDismissDialog: () -> Unit = {},
    onStartChatClick: (isFabClicked: Boolean) -> Unit = {},
    onScheduleMeeting: () -> Unit = {},
    onShowNextTooltip: (MeetingTooltipItem) -> Unit = {},
    onDismissForceAppUpdateDialog: () -> Unit = {},
) {
    val initialPage = if (showMeetingTab) ChatTab.MEETINGS.ordinal else ChatTab.CHATS.ordinal
    val context = LocalContext.current
    val scaffoldState = rememberScaffoldState()
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        initialPageOffsetFraction = 0f
    ) {
        ChatTab.entries.size
    }
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
                LegacyMegaTooltip(
                    titleText = stringResource(R.string.chat_schedule_meeting),
                    descriptionText = stringResource(R.string.meeting_list_tooltip_fab_description),
                    actionText = stringResource(R.string.button_permission_info),
                    showOnTop = true,
                    onDismissed = { onShowNextTooltip(MeetingTooltipItem.RECURRING_OR_PENDING) },
                ) {
                    FabButton(true, onStartChatClick)
                }
            } else {
                if ((state.hasAnyContact.not() && state.chats.isEmpty()) || state.chats.isNotEmpty() || pagerState.currentPage == ChatTab.MEETINGS.ordinal) {
                    FabButton(showFabButton, onStartChatClick)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            Tabs(
                pagerEnabled = false,
                pagerState = pagerState,
                selectedTabIndex = pagerState.currentPage,
                onTabSelected = {
                    if (pagerState.currentPage == it) {
                        scrollToTop = !scrollToTop
                        false
                    }
                    true
                }
            ) {
                ChatTab.entries.forEachIndexed { index, item ->
                    addTextTab(
                        text = stringResource(item.titleStringRes),
                        tag = item.name,
                        suffix = { activeColor, color ->
                            if (state.currentUnreadStatus?.toList()?.getOrNull(index) == true) {
                                Canvas(modifier = Modifier.size(4.dp)) {
                                    drawCircle(color = activeColor, center = Offset(20f, 20f))
                                }
                            }
                        },
                    )
                }
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                val isMeetingView = page == ChatTab.MEETINGS.ordinal
                val items = if (isMeetingView) {
                    filteredMeetings ?: state.meetings
                } else {
                    filteredChats ?: state.chats
                }
                val isLoading = if (isMeetingView)
                    state.areMeetingsLoading
                else
                    state.areChatsLoading

                ChatListView(
                    items = items,
                    isLoading = isLoading,
                    selectedIds = state.selectedIds,
                    scrollToTop = scrollToTop,
                    onItemClick = onItemClick,
                    isMeetingView = isMeetingView,
                    tooltip = state.tooltip,
                    isNew = state.isNewFeature,
                    onItemMoreClick = onItemMoreClick,
                    onItemSelected = onItemSelected,
                    onScrollInProgress = { showFabButton = !it },
                    onEmptyButtonClick = { onStartChatClick(false) },
                    onScheduleMeeting = onScheduleMeeting,
                    onShowNextTooltip = onShowNextTooltip,
                    hasAnyContact = state.hasAnyContact
                )
            }

            LaunchedEffect(state.searchQuery) {
                state.searchQuery?.takeIf(String::isNotBlank)?.let { searchQuery ->
                    if (pagerState.currentPage == ChatTab.CHATS.ordinal) {
                        filteredChats = state.chats.filter { item ->
                            item.matches(searchQuery)
                        }
                    } else {
                        filteredMeetings = state.meetings.filter { item ->
                            item.matches(searchQuery)
                        }
                    }
                } ?: run {
                    filteredChats = null
                    filteredMeetings = null
                }
            }

            LaunchedEffect(pagerState.currentPage) {
                onTabSelected(ChatTab.entries[pagerState.currentPage])
            }

            EventEffect(
                event = state.snackbarMessageContent, onConsumed = onResetStateSnackbarMessage
            ) { resId ->
                scaffoldState.snackbarHostState.showAutoDurationSnackbar(
                    context.resources.getString(
                        resId
                    )
                )
            }

            EventEffect(
                event = managementState.snackbarMessageContent,
                onConsumed = onResetManagementStateSnackbarMessage
            ) {
                scaffoldState.snackbarHostState.showAutoDurationSnackbar(it)
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
    if (state.showForceUpdateDialog) {
        ForceAppUpdateDialog(onDismiss = onDismissForceAppUpdateDialog)
    }
}

private fun ChatRoomItem.matches(searchQuery: String): Boolean =
    title.contains(searchQuery, true)
            || title.normalize().contains(searchQuery, true)
            || lastMessage?.contains(searchQuery, true) == true
            || lastMessage?.normalize()?.contains(searchQuery, true) == true

@Composable
private fun FabButton(showFabButton: Boolean, onStartChatClick: (isFabClicked: Boolean) -> Unit) {
    AnimatedVisibility(
        visible = showFabButton,
        enter = scaleIn(),
        exit = scaleOut(),
    ) {
        FloatingActionButton(onClick = { onStartChatClick(true) }) {
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
    OriginalTheme(isSystemInDarkTheme()) {
        ChatTabsView(
            state = ChatsTabState(currentUnreadStatus = true to false),
            managementState = ScheduledMeetingManagementUiState(),
        )
    }
}
