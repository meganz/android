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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import mega.privacy.android.app.presentation.extensions.paging.printLoadStates
import mega.privacy.android.app.presentation.meeting.chat.model.ChatUiState
import mega.privacy.android.app.presentation.meeting.chat.model.MessageListViewModel
import mega.privacy.android.app.presentation.meeting.chat.model.messages.UIMessageState
import mega.privacy.android.app.presentation.meeting.chat.model.messages.header.ChatUnreadHeaderMessage
import mega.privacy.android.app.presentation.meeting.chat.model.messages.management.ParticipantUiMessage
import mega.privacy.android.core.ui.controls.chat.messages.LoadingMessagesHeader
import mega.privacy.android.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import timber.log.Timber

@Composable
internal fun MessageListView(
    parameter: MessageListParameter,
) {
    MessageListView(
        uiState = parameter.uiState,
        scrollState = parameter.scrollState,
        bottomPadding = parameter.bottomPadding,
        onMoreReactionsClicked = parameter.onMoreReactionsClicked,
        onReactionClicked = parameter.onReactionClicked,
        onReactionLongClick = parameter.onReactionLongClick,
        onSendErrorClick = parameter.onSendErrorClick,
        onMessageLongClick = parameter.onMessageLongClick,
        onForwardClicked = parameter.onForwardClicked,
        onCanSelectChanged = parameter.onCanSelectChanged,
        selectMode = parameter.selectMode,
        selectedItems = parameter.selectedItems,
        selectItem = parameter.selectItem,
        deselectItem = parameter.deselectItem,
    )
}

@Composable
internal fun MessageListView(
    uiState: ChatUiState,
    scrollState: LazyListState,
    bottomPadding: Dp,
    onMoreReactionsClicked: (Long) -> Unit,
    onReactionClicked: (Long, String, List<UIReaction>) -> Unit,
    onReactionLongClick: (String, List<UIReaction>) -> Unit,
    onMessageLongClick: (TypedMessage) -> Unit,
    onForwardClicked: (TypedMessage) -> Unit,
    onSendErrorClick: (TypedMessage) -> Unit,
    onCanSelectChanged: (Boolean) -> Unit,
    selectMode: Boolean,
    selectedItems: Set<Long>,
    selectItem: (TypedMessage) -> Unit,
    deselectItem: (TypedMessage) -> Unit,
    viewModel: MessageListViewModel = hiltViewModel(),
) {
    val pagingItems = viewModel.pagedMessages.collectAsLazyPagingItems()
    Timber.d("Paging pagingItems load state: \n ${pagingItems.printLoadStates()}")
    Timber.d("Paging pagingItems count ${pagingItems.itemCount}")
    val state by viewModel.state.collectAsStateWithLifecycle()
    onCanSelectChanged(pagingItems.itemSnapshotList.any { it?.isSelectable == true })

    var isDataLoaded by remember {
        mutableStateOf(false)
    }

    val screenHeight = with(LocalDensity.current) {
        LocalConfiguration.current.screenHeightDp.dp.toPx()
    }

    var lastCacheUpdateTime by remember {
        mutableStateOf(emptyMap<Long, Long>())
    }

    LaunchedEffect(uiState.chat) {
        if (uiState.chat != null) {
            viewModel.setUnreadCount(uiState.chat.unreadCount)
        }
    }

    LaunchedEffect(isDataLoaded) {
        if (!state.isJumpingToLastSeenMessage && (uiState.chat?.unreadCount ?: 0) > 0) {
            pagingItems.itemSnapshotList.indexOfFirst {
                it is ChatUnreadHeaderMessage
            }.takeIf { it != -1 }?.let {
                scrollState.scrollToItem(it, -(screenHeight * 2 / 3).toInt())
                // make sure the list is scrolled to the correct position
                viewModel.onScrolledToLastSeenMessage()
                pagingItems.itemSnapshotList.firstOrNull()?.id?.let { lastMessageId ->
                    viewModel.setMessageSeen(lastMessageId)
                }
            }
        }
        viewModel.updateLatestMessageId(pagingItems.itemSnapshotList.firstOrNull()?.id ?: -1L)
    }

    LaunchedEffect(state.userUpdate) {
        state.userUpdate?.let { update ->
            val newLastCacheUpdateTime = lastCacheUpdateTime.toMutableMap()
            update.changes.forEach {
                newLastCacheUpdateTime[it.key.id] = System.currentTimeMillis()
            }
            lastCacheUpdateTime = newLastCacheUpdateTime
            viewModel.onUserUpdateHandled()
        }
    }

    LaunchedEffect(pagingItems) {
        snapshotFlow { scrollState.firstVisibleItemIndex }
            .collect {
                if (it <= 1) {
                    scrollState.scrollToItem(0)
                }
            }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        state = scrollState,
        contentPadding = PaddingValues(bottom = bottomPadding.coerceAtLeast(12.dp)),
        reverseLayout = true,
    ) {


        if (!isDataLoaded) {
            item {
                LoadingMessagesHeader()
            }
            if (
                pagingItems.loadState.prepend.endOfPaginationReached
                && pagingItems.loadState.mediator?.prepend?.endOfPaginationReached == true
            ) {
                isDataLoaded = true
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
                pagingItems[index]?.let { currentItem ->
                    val isInSelectMode = selectMode && currentItem.isSelectable
                    val messageState = UIMessageState(
                        chatTitle = uiState.title,
                        isOneToOne = !uiState.isGroup && !uiState.isMeeting,
                        scheduledMeeting = uiState.scheduledMeeting,
                        lastUpdatedCache = lastCacheUpdateTime[currentItem.userHandle]
                            ?: 0L,
                        isInSelectMode = isInSelectMode,
                        isChecked = isInSelectMode && currentItem.id in selectedItems,
                    )
                    val onSelectedChanged: (Boolean) -> Unit = { selected ->
                        if (selected) {
                            currentItem.message?.let { selectItem(it) }
                        } else {
                            currentItem.message?.let { deselectItem(it) }
                        }
                    }
                    Box(modifier = Modifier.sizeIn(minHeight = 10.dp)) {
                        currentItem.MessageListItem(
                            state = messageState,
                            onLongClick = {
                                if (uiState.haveWritePermission) onMessageLongClick(it)
                            },
                            onMoreReactionsClicked = onMoreReactionsClicked,
                            onReactionClicked = onReactionClicked,
                            onReactionLongClick = onReactionLongClick,
                            onForwardClicked = onForwardClicked,
                            onSelectedChanged = onSelectedChanged,
                            onSendErrorClicked = onSendErrorClick,
                        )
                    }
                }
            }
        }
    }
}


/**
 * Message list parameter
 *
 * @property uiState
 * @property scrollState
 * @property bottomPadding
 * @property onMessageLongClick
 * @property onMoreReactionsClicked
 * @property onReactionClicked
 * @property onReactionLongClick
 * @property onForwardClicked
 * @property onCanSelectChanged
 * @property selectMode
 * @property selectedItems
 * @property selectItem
 * @property deselectItem
 */
internal data class MessageListParameter(
    val uiState: ChatUiState,
    val scrollState: LazyListState,
    val bottomPadding: Dp,
    val onMessageLongClick: (TypedMessage) -> Unit,
    val onMoreReactionsClicked: (Long) -> Unit,
    val onReactionClicked: (Long, String, List<UIReaction>) -> Unit,
    val onReactionLongClick: (String, List<UIReaction>) -> Unit,
    val onForwardClicked: (TypedMessage) -> Unit,
    val onSendErrorClick: (TypedMessage) -> Unit,
    val onCanSelectChanged: (Boolean) -> Unit,
    val selectMode: Boolean,
    val selectedItems: Set<Long>,
    val selectItem: (TypedMessage) -> Unit,
    val deselectItem: (TypedMessage) -> Unit,
) {
}

