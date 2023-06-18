package mega.privacy.android.app.presentation.chat.list.view

import android.content.res.Configuration
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.StringUtils.toSpannedHtmlText
import mega.privacy.android.core.ui.controls.MegaEmptyView
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_012_white_alpha_012
import mega.privacy.android.domain.entity.chat.ChatItem
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

@Composable
fun ChatListView(
    modifier: Modifier = Modifier,
    items: List<ChatItem>,
    selectedIds: List<Long>,
    scrollToTop: Boolean,
    onItemClick: (Long) -> Unit = {},
    onItemMoreClick: (ChatItem) -> Unit = {},
    onItemSelected: (Long) -> Unit = {},
    onFirstItemVisible: (Boolean) -> Unit = {},
) {
    if (items.isEmpty()) {
        EmptyView(modifier = modifier)
    } else {
        ListView(
            modifier = modifier,
            items = items,
            selectedIds = selectedIds,
            scrollToTop = scrollToTop,
            onItemClick = onItemClick,
            onItemMoreClick = onItemMoreClick,
            onItemSelected = onItemSelected,
            onFirstItemVisible = onFirstItemVisible,
        )
    }
}

@Composable
private fun ListView(
    modifier: Modifier = Modifier,
    items: List<ChatItem>,
    selectedIds: List<Long>,
    scrollToTop: Boolean,
    onItemClick: (Long) -> Unit,
    onItemMoreClick: (ChatItem) -> Unit,
    onItemSelected: (Long) -> Unit,
    onFirstItemVisible: (Boolean) -> Unit,
) {
    val listState = rememberLazyListState()
    var selectionEnabled by remember { mutableStateOf(false) }
    var tick by remember { mutableStateOf(0) }

    LazyColumn(
        state = listState,
        modifier = modifier.testTag("ListView"),
    ) {
        itemsIndexed(
            items = items,
            key = { _, item -> item.chatId }
        ) { index: Int, item: ChatItem ->
            item.header?.takeIf { it.isNotBlank() }?.let { header ->
                if (index != 0) ChatItemDivider(short = true)
                ChatItemHeaderView(text = header)
            } ?: run {
                if (index != 0) ChatItemDivider(short = false)
            }

            ChatItemView(
                item = item,
                isSelected = selectionEnabled && selectedIds.contains(item.chatId),
                isSelectionEnabled = selectionEnabled,
                timestampUpdate = tick.takeIf { item.hasOngoingCall() },
                onItemClick = onItemClick,
                onItemMoreClick = onItemMoreClick,
                onItemSelected = onItemSelected,
            )
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { onFirstItemVisible(it == 0) }
    }

    LaunchedEffect(scrollToTop) {
        listState.animateScrollToItem(0)
    }

    LaunchedEffect(selectedIds) {
        selectionEnabled = selectedIds.isNotEmpty()
    }

    LaunchedEffect(items) {
        while (items.any(ChatItem::hasOngoingCall)) {
            delay(1.seconds)
            tick++
        }
    }
}

@Composable
private fun ChatItemDivider(short: Boolean) {
    Divider(
        modifier = Modifier.padding(start = if (short) 16.dp else 72.dp),
        color = MaterialTheme.colors.grey_alpha_012_white_alpha_012,
        thickness = 1.dp
    )
}

@Composable
private fun EmptyView(
    modifier: Modifier = Modifier,
) {
    Surface(modifier.testTag("EmptyView")) {
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
