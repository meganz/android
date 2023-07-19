package mega.privacy.android.app.presentation.chat.list.view

import android.content.res.Configuration
import android.view.MotionEvent
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.StringUtils.toSpannedHtmlText
import mega.privacy.android.core.ui.controls.MegaEmptyView
import mega.privacy.android.domain.entity.chat.ChatRoomItem
import java.util.Locale

@Composable
fun ChatListView(
    modifier: Modifier = Modifier,
    items: List<ChatRoomItem>,
    selectedIds: List<Long>,
    scrollToTop: Boolean,
    onItemClick: (Long) -> Unit = {},
    onItemMoreClick: (ChatRoomItem) -> Unit = {},
    onItemSelected: (Long) -> Unit = {},
    onFirstItemVisible: (Boolean) -> Unit = {},
    onScrollInProgress: (Boolean) -> Unit = {},
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
            onScrollInProgress = onScrollInProgress,
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ListView(
    modifier: Modifier = Modifier,
    items: List<ChatRoomItem>,
    selectedIds: List<Long>,
    scrollToTop: Boolean,
    onItemClick: (Long) -> Unit,
    onItemMoreClick: (ChatRoomItem) -> Unit,
    onItemSelected: (Long) -> Unit,
    onFirstItemVisible: (Boolean) -> Unit,
    onScrollInProgress: (Boolean) -> Unit,
) {
    val listState = rememberLazyListState()
    var selectionEnabled by remember { mutableStateOf(false) }
    var hasBeenTouched by remember { mutableStateOf(false) }

    LazyColumn(
        state = listState,
        modifier = modifier
            .testTag("ListView")
            .pointerInteropFilter { motionEvent ->
                if (!hasBeenTouched && motionEvent.action == MotionEvent.ACTION_DOWN) {
                    hasBeenTouched = true
                }
                false
            },
    ) {
        itemsIndexed(
            items = items,
            key = { _, item -> item.chatId }
        ) { index: Int, item: ChatRoomItem ->
            item.header?.takeIf(String::isNotBlank)?.let { header ->
                if (index != 0) ChatDivider(startPadding = 16.dp)
                ChatRoomItemHeaderView(text = header)
            } ?: run {
                if (index != 0) ChatDivider()
            }

            ChatRoomItemView(
                item = item,
                isSelected = selectionEnabled && selectedIds.contains(item.chatId),
                isSelectionEnabled = selectionEnabled,
                onItemClick = onItemClick,
                onItemMoreClick = onItemMoreClick,
                onItemSelected = onItemSelected,
            )
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .collectLatest(onScrollInProgress)
        snapshotFlow { listState.firstVisibleItemIndex }
            .collectLatest { onFirstItemVisible(it == 0) }
    }

    LaunchedEffect(scrollToTop) {
        listState.animateScrollToItem(0)
    }

    LaunchedEffect(selectedIds) {
        selectionEnabled = selectedIds.isNotEmpty()
    }

    LaunchedEffect(items) {
        if (!hasBeenTouched) {
            listState.scrollToItem(0)
        }
    }
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
