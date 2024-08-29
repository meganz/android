package mega.privacy.android.app.presentation.chat.list.view

import mega.privacy.android.icon.pack.R as IconR
import mega.privacy.android.shared.resources.R as sharedR
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.collectLatest
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.chat.ChatRoomItem
import mega.privacy.android.domain.entity.chat.MeetingTooltipItem
import mega.privacy.android.legacy.core.ui.controls.tooltips.LegacyMegaTooltip
import mega.privacy.android.shared.original.core.ui.controls.buttons.OutlinedWithoutBackgroundMegaButton
import mega.privacy.android.shared.original.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.shared.original.core.ui.controls.text.MegaSpannedClickableText
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.model.MegaSpanStyle
import mega.privacy.android.shared.original.core.ui.model.MegaSpanStyleWithAnnotation
import mega.privacy.android.shared.original.core.ui.model.SpanIndicator
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemeTabletLandscapePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor
import mega.privacy.mobile.analytics.event.InviteFriendsLearnMorePressedEvent

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
    hasAnyContact: Boolean = false,
    isLoading: Boolean = false,
    tooltip: MeetingTooltipItem = MeetingTooltipItem.NONE,
    onItemClick: (Long) -> Unit = {},
    onItemMoreClick: (ChatRoomItem) -> Unit = {},
    onItemSelected: (Long) -> Unit = {},
    onFirstItemVisible: (Boolean) -> Unit = {},
    onScrollInProgress: (Boolean) -> Unit = {},
    onEmptyButtonClick: () -> Unit = {},
    onScheduleMeeting: () -> Unit = {},
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
            if (!isLoading)
                EmptyView(
                    isMeetingView = isMeetingView,
                    onEmptyButtonClick = onEmptyButtonClick,
                    hasAnyContact = hasAnyContact,
                    onScheduleMeeting = onScheduleMeeting,
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
private fun OrientationSwapper(
    content: @Composable () -> Unit,
) {
    val portrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
    if (portrait) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            content()
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            content()
        }
    }
}

@Composable
private fun EmptyView(
    isMeetingView: Boolean,
    modifier: Modifier = Modifier,
    hasAnyContact: Boolean = false,
    onEmptyButtonClick: () -> Unit = {},
    onScheduleMeeting: () -> Unit = {},
) {
    val imageResource: Int
    val titleResource: Int
    val descriptionResource: Int
    val buttonResource: Int
    if (isMeetingView) {
        imageResource =
            if (isSystemInDarkTheme()) IconR.drawable.ic_meeting_video_dark else IconR.drawable.ic_meeting_video
        titleResource = sharedR.string.meeting_recent_list_empty_title
        descriptionResource = sharedR.string.meeting_recent_list_empty_subtitle
        buttonResource = R.string.action_start_meeting_now
    } else {
        imageResource =
            if (isSystemInDarkTheme()) IconR.drawable.ic_message_call_dark else IconR.drawable.ic_message_call
        titleResource = sharedR.string.chat_recent_list_empty_title
        descriptionResource = sharedR.string.chat_recent_list_empty_subtitle
        buttonResource =
            if (hasAnyContact) R.string.fab_label_new_chat else sharedR.string.chat_recent_invite_friend
    }

    Column(
        modifier = modifier
            .testTag("chat_room_list:empty")
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val isPortrait =
            LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
        if (isPortrait) {
            Image(
                painter = painterResource(imageResource),
                contentDescription = "Empty placeholder",
                modifier = Modifier.size(120.dp),
            )
        }
        MegaText(
            text = stringResource(titleResource),
            style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.W500),
            textColor = TextColor.Primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 10.dp, start = 50.dp, end = 50.dp)
        )

        val context = LocalContext.current

        MegaSpannedClickableText(
            value = stringResource(id = descriptionResource),
            styles = hashMapOf(
                SpanIndicator('A') to MegaSpanStyleWithAnnotation(
                    MegaSpanStyle(
                        SpanStyle(textDecoration = TextDecoration.Underline),
                        color = TextColor.Secondary,
                    ), "https://mega.io/chatandmeetings"
                ),
            ),
            color = TextColor.Secondary,
            onAnnotationClick = {
                Analytics.tracker.trackEvent(InviteFriendsLearnMorePressedEvent)
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://mega.io/chatandmeetings"))
                )
            },
            baseStyle = MaterialTheme.typography.body1.copy(
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.W400,
                fontSize = 14.sp
            ),
            modifier = Modifier.padding(top = 20.dp, start = 50.dp, end = 50.dp)
        )
        OrientationSwapper {
            RaisedDefaultMegaButton(
                textId = buttonResource,
                onClick = onEmptyButtonClick,
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .align(Alignment.CenterHorizontally)
            )
            if (isMeetingView) {
                OutlinedWithoutBackgroundMegaButton(
                    modifier = Modifier
                        .padding(start = 20.dp, end = 20.dp)
                        .align(Alignment.CenterHorizontally),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    text = stringResource(id = R.string.chat_schedule_meeting),
                    onClick = onScheduleMeeting,
                    rounded = false,
                    enabled = true,
                    iconId = null
                )
            }
        }
    }
}

@CombinedThemePreviews
@CombinedThemeTabletLandscapePreviews
@Composable
private fun PreviewMeetingEmptyView(
    @PreviewParameter(BooleanProvider::class) isMeeting: Boolean,
) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        ChatListView(
            items = emptyList(),
            selectedIds = emptyList(),
            isMeetingView = isMeeting,
            scrollToTop = false,
        )
    }
}
