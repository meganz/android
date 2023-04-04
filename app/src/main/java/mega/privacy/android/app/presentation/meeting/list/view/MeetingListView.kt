package mega.privacy.android.app.presentation.meeting.list.view

import android.content.res.Configuration
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.model.MeetingListState
import mega.privacy.android.app.utils.StringUtils.toSpannedHtmlText
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.core.ui.controls.MegaEmptyView
import mega.privacy.android.core.ui.theme.extensions.grey_012_white_012
import mega.privacy.android.domain.entity.chat.MeetingRoomItem
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

/**
 * Meeting list view
 *
 * @param state
 * @param onItemClick
 * @param onItemMoreClick
 */
@Composable
fun MeetingListView(
    state: MeetingListState,
    onItemClick: (Long) -> Unit = {},
    onItemMoreClick: (Long) -> Unit = {},
    onItemSelected: (Long) -> Unit = {},
) {
    if (state.meetings.isEmpty()) {
        EmptyView()
    } else {
        ListView(
            state = state,
            onItemClick = onItemClick,
            onItemMoreClick = onItemMoreClick,
            onItemSelected = onItemSelected,
        )
    }
}

/**
 * List view
 *
 * @param modifier
 * @param state
 * @param onItemClick
 * @param onItemMoreClick
 */
@Composable
private fun ListView(
    modifier: Modifier = Modifier,
    state: MeetingListState,
    onItemClick: (Long) -> Unit,
    onItemMoreClick: (Long) -> Unit,
    onItemSelected: (Long) -> Unit,
) {
    val listState = rememberLazyListState()
    var tick by remember { mutableStateOf(0) }
    var listInteraction by remember { mutableStateOf(false) }
    var showHeaders by remember { mutableStateOf(false) }
    val isSelectionEnabled = state.selectedMeetings.isNotEmpty()

    LazyColumn(
        state = listState,
        modifier = modifier
            .testTag("MeetingListView")
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { listInteraction = true }
                )
            }
    ) {
        itemsIndexed(
            items = state.meetings,
            key = { _, item -> item.chatId }
        ) { index: Int, item: MeetingRoomItem ->
            val isSelected = isSelectionEnabled && state.selectedMeetings.contains(item.chatId)
            if (showHeaders) {
                val previousItem = state.meetings.getOrNull(index - 1)
                MeetingHeader(
                    previousItem = previousItem,
                    item = item,
                    showTopDivider = index != 0
                )
            }
            MeetingItemView(
                meeting = item,
                isSelected = isSelected,
                isSelectionEnabled = isSelectionEnabled,
                timestampUpdate = if (item.hasOngoingCall()) tick else null,
                onItemClick = onItemClick,
                onItemMoreClick = onItemMoreClick,
                onItemSelected = onItemSelected,
            )
        }
    }

    LaunchedEffect(state.scrollToTop) {
        listState.animateScrollToItem(0)
    }

    LaunchedEffect(state.meetings) {
        if (!listInteraction) {
            listState.scrollToItem(0)
        }
        showHeaders = state.meetings.any(MeetingRoomItem::isPending)
        val ongoingCall = state.meetings.any(MeetingRoomItem::hasOngoingCall)
        while (ongoingCall) {
            delay(1.seconds)
            tick++
        }
    }
}

/**
 * Meeting item view header
 *
 * @param previousItem
 * @param item
 * @param showTopDivider
 */
@Composable
private fun MeetingHeader(
    previousItem: MeetingRoomItem?,
    item: MeetingRoomItem,
    showTopDivider: Boolean,
) {
    when {
        !item.isPending && previousItem?.isPending == true -> {
            MeetingHeaderItemView(
                text = stringResource(id = R.string.meetings_list_past_header),
                showDivider = showTopDivider
            )
        }
        item.isPending && !isSameDay(
            item.scheduledStartTimestamp,
            previousItem?.scheduledStartTimestamp
        ) -> {
            MeetingHeaderItemView(
                text = TimeUtils.formatDate(
                    item.scheduledStartTimestamp!!,
                    TimeUtils.DATE_WEEK_DAY_FORMAT,
                    true,
                    LocalContext.current,
                ),
                showDivider = showTopDivider
            )
        }
        else -> {
            Divider(
                modifier = Modifier.padding(start = 72.dp),
                color = MaterialTheme.colors.grey_012_white_012,
                thickness = 1.dp
            )
        }
    }
}

private fun isSameDay(timeStampA: Long?, timeStampB: Long?): Boolean =
    if (timeStampA == null || timeStampB == null) {
        false
    } else {
        val dayA = Instant.ofEpochSecond(timeStampA)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        val dayB = Instant.ofEpochSecond(timeStampB)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        dayA.isEqual(dayB)
    }

@Composable
private fun EmptyView() {
    Surface(Modifier.testTag("MeetingListEmptyView")) {
        MegaEmptyView(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_no_search_results),
            text = stringResource(R.string.no_results_found)
                .uppercase(Locale.getDefault())
                .toSpannedHtmlText()
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, name = "PreviewEmptyView")
@Composable
private fun PreviewEmptyView() {
    EmptyView()
}
