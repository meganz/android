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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.delay
import mega.privacy.android.app.presentation.extensions.paging.printLoadStates
import mega.privacy.android.app.presentation.meeting.chat.model.ChatUiState
import mega.privacy.android.app.presentation.meeting.chat.model.MessageListViewModel
import mega.privacy.android.app.presentation.meeting.chat.model.messages.UIMessageState
import mega.privacy.android.app.presentation.meeting.chat.model.messages.header.ChatUnreadHeaderMessage
import mega.privacy.android.app.presentation.meeting.chat.model.messages.management.ParticipantUiMessage
import mega.privacy.android.app.presentation.meeting.chat.view.message.FirstMessageHeader
import mega.privacy.android.core.ui.controls.chat.messages.LoadingMessagesHeader
import mega.privacy.android.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.domain.entity.chat.ChatMessageStatus
import mega.privacy.android.domain.entity.chat.messages.PendingAttachmentMessage
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
        onNotSentClick = parameter.onNotSentClick,
        onMessageLongClick = parameter.onMessageLongClick,
        onForwardClicked = parameter.onForwardClicked,
        onCanSelectChanged = parameter.onCanSelectChanged,
        selectMode = parameter.selectMode,
        selectedItems = parameter.selectedItems,
        selectItem = parameter.selectItem,
        deselectItem = parameter.deselectItem,
        showUnreadIndicator = parameter.showUnreadIndicator,
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
    onNotSentClick: (TypedMessage) -> Unit,
    onCanSelectChanged: (Boolean) -> Unit,
    selectMode: Boolean,
    selectedItems: Set<Long>,
    selectItem: (TypedMessage) -> Unit,
    deselectItem: (TypedMessage) -> Unit,
    showUnreadIndicator: (Int) -> Unit,
    viewModel: MessageListViewModel = hiltViewModel(),
) {
    val pagingItems = viewModel.pagedMessages.collectAsLazyPagingItems()
    Timber.d("Paging pagingItems load state: \n ${pagingItems.printLoadStates()}")
    Timber.d("Paging pagingItems count ${pagingItems.itemCount}")
    val state by viewModel.state.collectAsStateWithLifecycle()
    onCanSelectChanged(pagingItems.itemSnapshotList.any { it?.isSelectable == true })
    val isBottomReached by remember {
        derivedStateOf {
            scrollState.firstVisibleItemIndex == 0 && scrollState.firstVisibleItemScrollOffset == 0
        }
    }

    var isDataLoaded by remember { mutableStateOf(false) }
    var unreadHeaderPosition by remember { mutableIntStateOf(0) }

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

    LaunchedEffect(isBottomReached) {
        // if user is at the bottom, remove unread indicator
        if (isBottomReached && pagingItems.itemCount > 0) {
            viewModel.onScrollToLatestMessage()
            showUnreadIndicator(0)
        }
    }

    LaunchedEffect(state.receivedMessages) {
        if (state.receivedMessages.isNotEmpty()) {
            // auto scroll if user is at the bottom
            if (isBottomReached) {
                delay(300L)
                scrollState.scrollToItem(0, 0)
            } else { // otherwise show unread indicator
                showUnreadIndicator(state.receivedMessages.size)
            }
        }
    }

    LaunchedEffect(unreadHeaderPosition) {
        if (unreadHeaderPosition > 0) {
            scrollState.scrollToItem(unreadHeaderPosition, -(screenHeight * 2 / 3).toInt())
            viewModel.onScrolledToLastSeenMessage()
        }
    }

    LaunchedEffect(pagingItems.itemSnapshotList.lastOrNull()?.id, pagingItems.itemCount) {
        if (!state.isJumpingToLastSeenMessage && (uiState.chat?.unreadCount ?: 0) > 0) {
            pagingItems.itemSnapshotList.indexOfFirst {
                it is ChatUnreadHeaderMessage
            }.takeIf { it != -1 }?.let {
                unreadHeaderPosition = it
            }
        }
        val latestMessage = pagingItems.itemSnapshotList.firstOrNull()?.message
        // auto scroll if you just sent a message
        if (latestMessage?.isMine == true
            && (latestMessage.status == ChatMessageStatus.SENDING || latestMessage is PendingAttachmentMessage)
            && viewModel.latestMessageId.longValue != latestMessage.msgId
        ) {
            scrollState.scrollToItem(0, 0)
        }
        viewModel.updateLatestMessage(pagingItems.itemSnapshotList)
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

    if (pagingItems.loadState.prepend.endOfPaginationReached) {
        isDataLoaded = true
    }

    if (!isDataLoaded) {
        LoadingMessagesHeader()
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            state = scrollState,
            contentPadding = PaddingValues(bottom = bottomPadding.coerceAtLeast(12.dp)),
            reverseLayout = true,
        ) {
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
                            onNotSentClick = onNotSentClick,
                        )
                    }
                }
            }
            // fix issue empty header for empty group
            if (pagingItems.loadState.mediator?.append?.endOfPaginationReached == true) {
                item {
                    FirstMessageHeader(uiState.title, uiState.scheduledMeeting)
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
    val onNotSentClick: (TypedMessage) -> Unit,
    val onCanSelectChanged: (Boolean) -> Unit,
    val selectMode: Boolean,
    val selectedItems: Set<Long>,
    val selectItem: (TypedMessage) -> Unit,
    val deselectItem: (TypedMessage) -> Unit,
    val showUnreadIndicator: (Int) -> Unit,
)

