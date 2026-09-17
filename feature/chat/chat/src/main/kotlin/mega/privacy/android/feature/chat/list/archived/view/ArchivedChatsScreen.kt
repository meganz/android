package mega.privacy.android.feature.chat.list.archived.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import kotlinx.collections.immutable.persistentListOf
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.android.core.ui.components.state.EmptyStateView
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.chat.list.archived.model.ArchivedChatsUiState
import mega.privacy.android.feature.chat.list.model.ChatListBulkActionsUiState
import mega.privacy.android.feature.chat.list.model.ChatRoomActionUiItem
import mega.privacy.android.feature.chat.list.model.ChatRoomUiItem
import mega.privacy.android.feature.chat.list.view.ChatRoomActionsOverlay
import mega.privacy.android.feature.chat.list.view.ChatRoomItemView
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.chats.components.ChatsViewSkeleton
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Archived chats screen listing the user's archived chat rooms.
 *
 * @param uiState UI state of the screen.
 * @param onBackClick Callback when back navigation is requested.
 * @param onItemClick Callback when an archived chat row is clicked, with the chat id.
 * @param modifier [Modifier]
 * @param resolveChatRoomActions Resolves the ordered actions available for the given chats.
 * @param onChatRoomActionSelected Runs the picked action across the given chats.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ArchivedChatsScreen(
    uiState: ArchivedChatsUiState,
    onBackClick: () -> Unit,
    onItemClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    resolveChatRoomActions: suspend (Set<Long>) -> List<ChatRoomActionUiItem> = { emptyList() },
    onChatRoomActionSelected: (Set<Long>, String) -> Unit = { _, _ -> },
) {
    var actionsState by retain {
        mutableStateOf<ChatListBulkActionsUiState>(ChatListBulkActionsUiState.Hidden)
    }

    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = modifier
            .fillMaxSize()
            .testTag(ARCHIVED_CHATS_SCREEN_TAG),
        topBar = {
            MegaTopAppBar(
                modifier = Modifier.testTag(ARCHIVED_CHATS_APP_BAR_TAG),
                title = stringResource(sharedR.string.chat_archived_chats_title),
                navigationType = AppBarNavigationType.Back(onBackClick),
            )
        },
    ) { paddingValues ->
        when (uiState) {
            ArchivedChatsUiState.Loading -> ChatsViewSkeleton(
                modifier = Modifier
                    .padding(paddingValues)
                    .testTag(ARCHIVED_CHATS_LOADING_TAG),
            )

            ArchivedChatsUiState.Empty -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .testTag(ARCHIVED_CHATS_EMPTY_TAG),
                contentAlignment = Alignment.Center,
            ) {
                EmptyStateView(
                    illustration = iconPackR.drawable.ic_message_call_glass,
                    title = stringResource(sharedR.string.chat_archived_chats_empty_title),
                )
            }

            is ArchivedChatsUiState.Data -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(ARCHIVED_CHATS_LIST_TAG),
                contentPadding = paddingValues,
            ) {
                items(items = uiState.items, key = ChatRoomUiItem::chatId) { item ->
                    ChatRoomItemView(
                        item = item,
                        onItemClick = onItemClick,
                        onItemMoreClick = { chatId ->
                            actionsState =
                                ChatListBulkActionsUiState.ShowingActions(setOf(chatId))
                        },
                    )
                }
            }
        }
    }

    ChatRoomActionsOverlay(
        actionsState = actionsState,
        resolveActions = resolveChatRoomActions,
        onStateChange = { actionsState = it },
        onActionSelected = onChatRoomActionSelected,
    )
}

internal const val ARCHIVED_CHATS_SCREEN_TAG = "archived_chats_screen:screen"
internal const val ARCHIVED_CHATS_APP_BAR_TAG = "archived_chats_screen:app_bar"
internal const val ARCHIVED_CHATS_LOADING_TAG = "archived_chats_screen:loading"
internal const val ARCHIVED_CHATS_EMPTY_TAG = "archived_chats_screen:empty"
internal const val ARCHIVED_CHATS_LIST_TAG = "archived_chats_screen:list"

private val previewArchivedChats = persistentListOf(
    ChatRoomUiItem(
        chatId = 1L,
        title = "Mieko Kawakami",
        lastMessage = "See you tomorrow!",
        lastTimestampFormatted = "Today 14:25",
        header = null,
        pendingMeeting = null,
        showRecurringIcon = false,
        unreadCount = 0,
        isMuted = false,
        highlight = false,
        isNoteToSelf = false,
        avatar = ChatRoomUiItem.ChatRoomUiAvatar.Peer(
            placeholderText = "M",
            filePath = null,
            color = 0xFFFEBC00.toInt(),
        ),
        status = ContactItemStatus.Unknown,
        hasOngoingCall = false,
    ),
    ChatRoomUiItem(
        chatId = 2L,
        title = "Recipe test #14",
        lastMessage = "Anna: Seeya all soon!",
        lastTimestampFormatted = "1 May 2022 17:53",
        header = null,
        pendingMeeting = null,
        showRecurringIcon = false,
        unreadCount = 0,
        isMuted = false,
        highlight = false,
        isNoteToSelf = false,
        avatar = ChatRoomUiItem.ChatRoomUiAvatar.Group,
        status = ContactItemStatus.Unknown,
        hasOngoingCall = false,
    ),
)

@CombinedThemePreviews
@Composable
private fun ArchivedChatsScreenPreview() {
    AndroidThemeForPreviews {
        ArchivedChatsScreen(
            uiState = ArchivedChatsUiState.Data(previewArchivedChats),
            onBackClick = {},
            onItemClick = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ArchivedChatsScreenEmptyPreview() {
    AndroidThemeForPreviews {
        ArchivedChatsScreen(
            uiState = ArchivedChatsUiState.Empty,
            onBackClick = {},
            onItemClick = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ArchivedChatsScreenLoadingPreview() {
    AndroidThemeForPreviews {
        ArchivedChatsScreen(
            uiState = ArchivedChatsUiState.Loading,
            onBackClick = {},
            onItemClick = {},
        )
    }
}
