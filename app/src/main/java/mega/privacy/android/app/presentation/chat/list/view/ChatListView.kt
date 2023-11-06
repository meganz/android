package mega.privacy.android.app.presentation.chat.list.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import mega.privacy.android.app.R
import mega.privacy.android.legacy.core.ui.controls.tooltips.LegacyMegaTooltip
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_054_white_alpha_054
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_087_white_alpha_087
import mega.privacy.android.domain.entity.chat.ChatRoomItem
import mega.privacy.android.domain.entity.chat.MeetingTooltipItem

/**
 * Chat list view
 *
 * @param modifier
 * @param items
 * @param selectedIds
 * @param scrollToTop
 * @param isMeetingView
 * @param tooltip
 * @param onItemClick
 * @param onItemMoreClick
 * @param onItemSelected
 * @param onFirstItemVisible
 * @param onScrollInProgress
 * @param onEmptyButtonClick
 * @param onShowNextTooltip
 */
@Composable
fun ChatListView(
    items: List<ChatRoomItem>,
    selectedIds: List<Long>,
    scrollToTop: Boolean,
    isMeetingView: Boolean,
    modifier: Modifier = Modifier,
    tooltip: MeetingTooltipItem = MeetingTooltipItem.NONE,
    onItemClick: (Long) -> Unit = {},
    onItemMoreClick: (ChatRoomItem) -> Unit = {},
    onItemSelected: (Long) -> Unit = {},
    onFirstItemVisible: (Boolean) -> Unit = {},
    onScrollInProgress: (Boolean) -> Unit = {},
    onEmptyButtonClick: () -> Unit = {},
    onShowNextTooltip: (MeetingTooltipItem) -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.surface)
    ) {
        if (items.isNotEmpty()) {
            ListView(
                items = items,
                selectedIds = selectedIds,
                scrollToTop = scrollToTop,
                tooltip = tooltip,
                onItemClick = onItemClick,
                onItemMoreClick = onItemMoreClick,
                onItemSelected = onItemSelected,
                onFirstItemVisible = onFirstItemVisible,
                onScrollInProgress = onScrollInProgress,
                onShowNextTooltip = onShowNextTooltip,
            )
        } else {
            EmptyView(
                isMeetingView = isMeetingView,
                onEmptyButtonClick = onEmptyButtonClick,
            )
        }
    }
}

@Composable
private fun ListView(
    items: List<ChatRoomItem>,
    selectedIds: List<Long>,
    scrollToTop: Boolean,
    tooltip: MeetingTooltipItem,
    onItemClick: (Long) -> Unit,
    onItemMoreClick: (ChatRoomItem) -> Unit,
    onItemSelected: (Long) -> Unit,
    onFirstItemVisible: (Boolean) -> Unit,
    onScrollInProgress: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onShowNextTooltip: (MeetingTooltipItem) -> Unit = {},
) {
    val listState = rememberLazyListState()
    var selectionEnabled by remember { mutableStateOf(false) }
    var showTooltip = tooltip == MeetingTooltipItem.RECURRING_OR_PENDING
            || tooltip == MeetingTooltipItem.RECURRING || tooltip == MeetingTooltipItem.PENDING

    LazyColumn(
        state = listState,
        modifier = modifier.testTag("chat_room_list:list")
    ) {
        itemsIndexed(
            items = items,
            key = { _, item -> item.chatId }
        ) { index: Int, item: ChatRoomItem ->
            item.header?.takeIf(String::isNotBlank)?.let { header ->
                if (index != 0) ChatDivider(startPadding = 16.dp)
                ChatRoomItemHeaderView(
                    modifier = modifier.testTag("chat_room_list:item_header"),
                    text = header
                )
            } ?: run {
                if (index != 0) ChatDivider()
            }

            TooltipView(
                tooltip = tooltip,
                showTooltip = showTooltip,
                itemIsRecurring = item is ChatRoomItem.MeetingChatRoomItem && item.isRecurring() && item.hasPermissions,
                itemIsPending = item is ChatRoomItem.MeetingChatRoomItem && item.isPending,
                onShowNextTooltip = onShowNextTooltip,
                onTooltipShown = { showTooltip = false },
            ) {
                ChatRoomItemView(
                    modifier = modifier.testTag("chat_room_list:item"),
                    item = item,
                    isSelected = selectionEnabled && selectedIds.contains(item.chatId),
                    isSelectionEnabled = selectionEnabled,
                    onItemClick = onItemClick,
                    onItemMoreClick = onItemMoreClick,
                    onItemSelected = onItemSelected,
                )
            }
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .collectLatest(onScrollInProgress)
        snapshotFlow { listState.firstVisibleItemIndex }
            .collectLatest { onFirstItemVisible(it == 0) }
    }

    LaunchedEffect(items.firstOrNull()) {
        if (!listState.isScrollInProgress && listState.firstVisibleItemIndex in 1..4) {
            listState.scrollToItem(0)
        }
    }

    LaunchedEffect(scrollToTop) {
        listState.animateScrollToItem(0)
    }

    LaunchedEffect(selectedIds) {
        selectionEnabled = selectedIds.isNotEmpty()
    }
}

@Composable
private fun TooltipView(
    tooltip: MeetingTooltipItem,
    showTooltip: Boolean,
    itemIsRecurring: Boolean,
    itemIsPending: Boolean,
    onShowNextTooltip: (MeetingTooltipItem) -> Unit,
    onTooltipShown: () -> Unit,
    content: @Composable () -> Unit,
) {
    when {
        showTooltip && (tooltip == MeetingTooltipItem.RECURRING_OR_PENDING
                || tooltip == MeetingTooltipItem.RECURRING) && itemIsRecurring -> {
            onTooltipShown()
            LegacyMegaTooltip(
                modifier = Modifier.testTag("chat_room_list:tooltip_recurring"),
                titleText = stringResource(R.string.meeting_list_tooltip_recurring_title),
                descriptionText = stringResource(R.string.meeting_list_tooltip_recurring_description),
                actionText = stringResource(R.string.button_permission_info),
                showOnTop = false,
                content = content,
                onDismissed = {
                    if (tooltip == MeetingTooltipItem.RECURRING_OR_PENDING) {
                        onShowNextTooltip(MeetingTooltipItem.PENDING)
                    } else {
                        onShowNextTooltip(MeetingTooltipItem.NONE)
                    }
                }
            )
        }

        showTooltip && (tooltip == MeetingTooltipItem.RECURRING_OR_PENDING
                || tooltip == MeetingTooltipItem.PENDING) && itemIsPending -> {
            onTooltipShown()
            LegacyMegaTooltip(
                modifier = Modifier.testTag("chat_room_list:tooltip_start"),
                titleText = stringResource(R.string.btn_start_meeting),
                descriptionText = stringResource(R.string.meeting_list_tooltip_sched_description),
                actionText = stringResource(R.string.button_permission_info),
                showOnTop = false,
                content = content,
                onDismissed = {
                    if (tooltip == MeetingTooltipItem.RECURRING_OR_PENDING) {
                        onShowNextTooltip(MeetingTooltipItem.RECURRING)
                    } else {
                        onShowNextTooltip(MeetingTooltipItem.NONE)
                    }
                }
            )
        }

        else -> content()
    }
}

@Composable
private fun EmptyView(
    isMeetingView: Boolean,
    modifier: Modifier = Modifier,
    onEmptyButtonClick: () -> Unit = {},
) {
    val imageResource: Int
    val titleResource: Int
    val descriptionResource: Int
    val buttonResource: Int
    if (isMeetingView) {
        imageResource = R.drawable.ic_zero_meeting
        titleResource = R.string.meeting_list_empty_action
        descriptionResource = R.string.meeting_list_empty_description
        buttonResource = R.string.new_meeting
    } else {
        imageResource = R.drawable.ic_zero_chat
        titleResource = R.string.chat_recent_list_empty_action
        descriptionResource = R.string.chat_recent_list_empty_description
        buttonResource = R.string.fab_label_new_chat
    }

    Column(
        modifier = modifier
            .testTag("chat_room_list:empty")
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(imageResource),
            contentDescription = "Empty placeholder",
            modifier = Modifier.size(144.dp)
        )

        Text(
            text = stringResource(titleResource),
            style = MaterialTheme.typography.subtitle1,
            color = MaterialTheme.colors.grey_alpha_087_white_alpha_087,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 10.dp, start = 50.dp, end = 50.dp)
        )

        Text(
            text = stringResource(descriptionResource),
            style = MaterialTheme.typography.subtitle2,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.grey_alpha_054_white_alpha_054,
            modifier = Modifier.padding(top = 16.dp, start = 50.dp, end = 50.dp)
        )

        OutlinedButton(
            onClick = onEmptyButtonClick,
            colors = ButtonDefaults.outlinedButtonColors(
                backgroundColor = Color.Transparent,
                contentColor = MaterialTheme.colors.secondary
            ),
            border = BorderStroke(width = 1.dp, color = MaterialTheme.colors.secondary),
            modifier = Modifier
                .padding(vertical = 40.dp)
                .align(Alignment.CenterHorizontally)
        ) {
            Text(
                text = stringResource(buttonResource),
                style = MaterialTheme.typography.button,
                color = MaterialTheme.colors.secondary
            )
        }
    }
}

@Preview
@Composable
private fun PreviewChatEmptyView() {
    ChatListView(
        items = emptyList(),
        selectedIds = emptyList(),
        isMeetingView = false,
        scrollToTop = false,
    )
}

@Preview
@Composable
private fun PreviewMeetingEmptyView() {
    ChatListView(
        items = emptyList(),
        selectedIds = emptyList(),
        isMeetingView = true,
        scrollToTop = false,
    )
}
