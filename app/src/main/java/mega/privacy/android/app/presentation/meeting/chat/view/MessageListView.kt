package mega.privacy.android.app.presentation.meeting.chat.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import mega.privacy.android.app.presentation.meeting.chat.model.ChatUiState
import mega.privacy.android.app.presentation.meeting.chat.model.MessageListViewModel
import mega.privacy.android.app.presentation.meeting.chat.model.messages.management.ParticipantUiMessage
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.core.ui.controls.chat.messages.LoadingMessagesHeader
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import timber.log.Timber

@Composable
internal fun MessageListView(
    uiState: ChatUiState,
    scrollState: LazyListState,
    bottomPadding: Dp,
    viewModel: MessageListViewModel = hiltViewModel(),
    onUserUpdateHandled: () -> Unit = {},
    onMessageLongClick: (TypedMessage) -> Unit = {},
) {
    val pagingItems = viewModel.pagedMessages.collectAsLazyPagingItems()
    Timber.d("Paging pagingItems load state ${pagingItems.loadState}")
    Timber.d("Paging pagingItems count ${pagingItems.itemCount}")

    var lastCacheUpdateTime by remember {
        mutableStateOf(emptyMap<Long, Long>())
    }
    var scrollToBottom by remember { mutableStateOf(true) }
    val derivedScrollToBottom by remember {
        derivedStateOf {
            scrollToBottom &&
                    pagingItems.loadState.refresh is LoadState.NotLoading &&
                    pagingItems.itemCount > 0
        }
    }

    LaunchedEffect(pagingItems.itemSnapshotList) {
        viewModel.updateLatestMessageId(pagingItems.itemSnapshotList.lastOrNull()?.id ?: -1L)
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

    LaunchedEffect(key1 = derivedScrollToBottom) {
        Timber.d("Paging derivedScrollToBottom is $derivedScrollToBottom")
        if (derivedScrollToBottom) {
            scrollState.scrollToItem(
                index = pagingItems.itemCount - 1
            )
            scrollToBottom = false
        }
    }
    val context = LocalContext.current


    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        state = scrollState,
        contentPadding = PaddingValues(bottom = bottomPadding.coerceAtLeast(12.dp))
    ) {
        item("header") {
            AnimatedVisibility(visible = pagingItems.loadState.refresh is LoadState.Loading) {
                LoadingMessagesHeader()
            }
        }
        when {
            pagingItems.loadState.refresh is LoadState.Error -> {
//                Error view
            }

            pagingItems.loadState.prepend is LoadState.Error -> {
                // Loading previous messages failed
            }

            pagingItems.loadState.prepend is LoadState.NotLoading
                    && pagingItems.loadState.refresh is LoadState.NotLoading
                    && pagingItems.loadState.prepend.endOfPaginationReached -> {

            }
        }

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
            pagingItems[index]?.MessageListItem(
                uiState = uiState,
                lastUpdatedCache = lastCacheUpdateTime[pagingItems[index]?.userHandle] ?: 0L,
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
            )
        }
    }
}