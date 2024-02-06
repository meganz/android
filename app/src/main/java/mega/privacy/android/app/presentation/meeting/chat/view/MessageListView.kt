package mega.privacy.android.app.presentation.meeting.chat.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import mega.privacy.android.app.presentation.extensions.paging.printLoadStates
import mega.privacy.android.app.presentation.meeting.chat.model.ChatUiState
import mega.privacy.android.app.presentation.meeting.chat.model.MessageListViewModel
import mega.privacy.android.app.presentation.meeting.chat.model.messages.management.ParticipantUiMessage
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.core.ui.controls.chat.messages.LoadingMessagesHeader
import mega.privacy.android.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import timber.log.Timber

@Composable
internal fun MessageListView(
    uiState: ChatUiState,
    scrollState: LazyListState,
    bottomPadding: Dp,
    onMoreReactionsClicked: (Long) -> Unit,
    onReactionClicked: (Long, String, List<UIReaction>) -> Unit,
    viewModel: MessageListViewModel = hiltViewModel(),
    onUserUpdateHandled: () -> Unit = {},
    onMessageLongClick: (TypedMessage) -> Unit = {},
) {
    val pagingItems = viewModel.pagedMessages.collectAsLazyPagingItems()
    Timber.d("Paging pagingItems load state: \n ${pagingItems.printLoadStates()}")
    Timber.d("Paging pagingItems count ${pagingItems.itemCount}")

    var lastCacheUpdateTime by remember {
        mutableStateOf(emptyMap<Long, Long>())
    }

    LaunchedEffect(pagingItems.itemSnapshotList) {
        viewModel.updateLatestMessageId(pagingItems.itemSnapshotList.lastOrNull()?.id ?: -1L)
    }

    LaunchedEffect(pagingItems) {
        snapshotFlow { scrollState.firstVisibleItemIndex }
            .collect {
                if (it <= 1) {
                    scrollState.scrollToItem(0)
                }
            }
    }

    LaunchedEffect(uiState.userUpdate) {
        if (uiState.userUpdate != null) {
            val newLastCacheUpdateTime = lastCacheUpdateTime.toMutableMap()
            uiState.userUpdate.changes.forEach {
                newLastCacheUpdateTime[it.key.id] = System.currentTimeMillis()
            }
            lastCacheUpdateTime = newLastCacheUpdateTime
            onUserUpdateHandled()
        }
    }

    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        state = scrollState,
        contentPadding = PaddingValues(bottom = bottomPadding.coerceAtLeast(12.dp)),
        reverseLayout = true,
    ) {

        if (pagingItems.itemSnapshotList.isEmpty() && pagingItems.loadState.refresh is LoadState.Loading) {
            item {
                LoadingMessagesHeader()
            }
        } else {
            items(
                count = pagingItems.itemCount,
                key = pagingItems.itemKey {
                    if (it is ParticipantUiMessage) {
                        // some messages we show the name of 2 users
                        "${it.key()}_${lastCacheUpdateTime[it.userHandle]}_${lastCacheUpdateTime[it.handleOfAction]}"
                    } else {
                        "${it.key()}_${lastCacheUpdateTime[it.userHandle]}"
                    }
                },
                contentType = pagingItems.itemContentType()
            ) { index ->

                Box(modifier = Modifier.sizeIn(minHeight = 42.dp)) {
                    pagingItems[index]?.MessageListItem(
                        uiState = uiState,
                        lastUpdatedCache = lastCacheUpdateTime[pagingItems[index]?.userHandle]
                            ?: 0L,
                        timeFormatter = TimeUtils::formatTime,
                        dateFormatter = {
                            TimeUtils.formatDate(
                                it,
                                TimeUtils.DATE_SHORT_FORMAT,
                                context
                            )
                        },
                        onLongClick = {
                            if (uiState.haveWritePermission) onMessageLongClick(it)
                        },
                        onMoreReactionsClicked = onMoreReactionsClicked,
                        onReactionClicked = onReactionClicked,
                    )
                }
            }
        }
    }
}


