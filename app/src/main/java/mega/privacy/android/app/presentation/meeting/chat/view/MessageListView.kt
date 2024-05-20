package mega.privacy.android.app.presentation.meeting.chat.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.IntState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import kotlinx.coroutines.delay
import mega.privacy.android.app.presentation.extensions.paging.printLoadStates
import mega.privacy.android.app.presentation.meeting.chat.model.ChatUiState
import mega.privacy.android.app.presentation.meeting.chat.model.MessageListViewModel
import mega.privacy.android.app.presentation.meeting.chat.model.messages.AvatarMessage
import mega.privacy.android.app.presentation.meeting.chat.model.messages.UIMessageState
import mega.privacy.android.app.presentation.meeting.chat.model.messages.UiChatMessage
import mega.privacy.android.app.presentation.meeting.chat.model.messages.header.ChatUnreadHeaderMessage
import mega.privacy.android.app.presentation.meeting.chat.model.messages.management.ParticipantUiMessage
import mega.privacy.android.app.presentation.meeting.chat.view.message.FirstMessageHeader
import mega.privacy.android.shared.original.core.ui.controls.chat.messages.LoadingMessagesHeader
import mega.privacy.android.shared.original.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.domain.entity.chat.ChatMessageStatus
import mega.privacy.android.domain.entity.chat.messages.PendingAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import timber.log.Timber

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
    navHostController: NavHostController,
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

    val lastVisibleIndex = remember { mutableIntStateOf(-1) }
    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.layoutInfo.visibleItemsInfo }
            .collect { visibleItems ->
                lastVisibleIndex.intValue = visibleItems.firstOrNull()?.index ?: -1
            }
    }
    val actualBottomPadding = bottomPadding.coerceAtLeast(12.dp)

    val lastItemAvatarPosition by computeLastItemAvatarPosition(
        pagingItems, scrollState, lastVisibleIndex, actualBottomPadding
    )

    if (pagingItems.loadState.prepend.endOfPaginationReached) {
        isDataLoaded = true
    }

    if (!isDataLoaded) {
        LoadingMessagesHeader()
    } else {
        // Fixed avatar to simulate it's scrolling with the list
        if (!selectMode && lastItemAvatarPosition == LastItemAvatarPosition.Scrolling) {
            pagingItems.peekOrNull(lastVisibleIndex.intValue)?.let {
                Box(modifier = Modifier.fillMaxSize()) {
                    ChatAvatar(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 16.dp, bottom = 0.dp),
                        handle = it.userHandle,
                        lastUpdatedCache = lastCacheUpdateTime[it.userHandle] ?: 0L
                    )
                }
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            state = scrollState,
            contentPadding = PaddingValues(bottom = actualBottomPadding),
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
                        lastItemAvatarPosition = if (index == lastVisibleIndex.intValue) {
                            lastItemAvatarPosition
                        } else if (!currentItem.displayAsMine
                            && currentItem is AvatarMessage
                            && pagingItems.peekOrNull(index - 1) !is AvatarMessage
                        ) {
                            LastItemAvatarPosition.Bottom
                        } else {
                            null
                        },
                    )
                    val onSelectedChanged: (Boolean) -> Unit = { selected ->
                        if (selected) {
                            currentItem.message?.let { selectItem(it) }
                        } else {
                            currentItem.message?.let { deselectItem(it) }
                        }
                    }
                    val haptic = LocalHapticFeedback.current
                    Box(modifier = Modifier.sizeIn(minHeight = 10.dp)) {
                        currentItem.MessageListItem(
                            state = messageState,
                            onLongClick = {
                                if (uiState.haveWritePermission) {
                                    onMessageLongClick(it)
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            },
                            onMoreReactionsClicked = onMoreReactionsClicked,
                            onReactionClicked = onReactionClicked,
                            onReactionLongClick = onReactionLongClick,
                            onForwardClicked = onForwardClicked,
                            onSelectedChanged = onSelectedChanged,
                            onNotSentClick = onNotSentClick,
                            navHostController = navHostController,
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

@Composable
private fun computeLastItemAvatarPosition(
    pagingItems: LazyPagingItems<UiChatMessage>,
    scrollState: LazyListState,
    lastVisibleIndex: IntState,
    bottomPadding: Dp,
): State<LastItemAvatarPosition?> {
    val avatarHeight: Float
    val bottomPaddingPixels: Float
    with(LocalDensity.current) {
        avatarHeight = 20.dp.toPx()
        bottomPaddingPixels = bottomPadding.toPx()
    }
    val minSizeToAnimate = avatarHeight * 2
    return remember {
        derivedStateOf {
            (pagingItems.peekOrNull(lastVisibleIndex.intValue) as? AvatarMessage)
                ?.takeIf { !it.displayAsMine && it.userHandle != -1L }
                ?.let { lastVisibleMessage ->
                    val visibleItems = scrollState.layoutInfo.visibleItemsInfo
                    val lastVisibleItem = visibleItems.firstOrNull() ?: return@derivedStateOf null
                    val secondLastVisibleItem =
                        visibleItems.getOrNull(1) ?: return@derivedStateOf null
                    val secondLastVisibleMessage =
                        pagingItems.peekOrNull(lastVisibleIndex.intValue + 1) as? AvatarMessage
                    val nextMessage =
                        pagingItems.peekOrNull(lastVisibleIndex.intValue - 1) as? AvatarMessage
                    val lastVisibleHasEnoughSpaceForAvatar =
                        secondLastVisibleItem.offset > avatarHeight
                    val lastVisibleIsFullyBVisible = bottomPaddingPixels > -lastVisibleItem.offset
                    val singleMessage =
                        lastVisibleMessage.userHandle != nextMessage?.userHandle && lastVisibleMessage.userHandle != secondLastVisibleMessage?.userHandle
                    when {
                        singleMessage && lastVisibleItem.size < minSizeToAnimate -> LastItemAvatarPosition.Bottom // No animation when it's only one small message
                        lastVisibleMessage.userHandle != secondLastVisibleMessage?.userHandle && !lastVisibleHasEnoughSpaceForAvatar -> LastItemAvatarPosition.Top // Last visible message is the first message of the user in this block and there's not enough space (it's disappearing down the screen)
                        lastVisibleIndex.intValue == 0 && lastVisibleMessage.userHandle != nextMessage?.userHandle && lastVisibleIsFullyBVisible -> LastItemAvatarPosition.Bottom // Need to take content padding into account for the very last message
                        else -> LastItemAvatarPosition.Scrolling// The avatar will be drawn in the list to simulate scrolling
                    }
                }
        }
    }
}

private fun LazyPagingItems<UiChatMessage>.peekOrNull(index: Int) =
    index.takeIf { it in 0..<this.itemCount }?.let { this.peek(it) }