package mega.privacy.android.feature.cloudexplorer.presentation.chatexplorer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.list.FlexibleLineListItem
import mega.android.core.ui.components.state.EmptyStateView
import mega.android.core.ui.components.tabs.TabsScope
import mega.android.core.ui.extensions.showAutoDurationSnackbar
import mega.android.core.ui.model.TabItems
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.domain.entity.chat.ChatStatus
import mega.privacy.android.feature.cloudexplorer.presentation.explorer.CHAT_TAB_TAG
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.navigation.destination.CreateGroupChatNavKey
import mega.privacy.android.shared.chats.components.ChatExplorerListItemView
import mega.privacy.android.shared.chats.model.ChatExplorerUiItem
import mega.privacy.android.shared.resources.R as sharedR

@Composable
internal fun ChatExplorerContent(
    uiState: ChatExplorerUiState,
    selectedChatIds: Set<Long>,
    onNewGroupChatClick: () -> Unit,
    onChatToggled: (chatId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        ChatExplorerUiState.Loading -> LoadingState(modifier = modifier)
        is ChatExplorerUiState.Data -> if (uiState.isEmpty) {
            EmptyView(
                data = uiState,
                selectedChatIds = selectedChatIds,
                onNewGroupChatClick = onNewGroupChatClick,
                onChatToggled = onChatToggled,
                modifier = modifier,
            )
        } else {
            ChatExplorerList(
                data = uiState,
                selectedChatIds = selectedChatIds,
                onNewGroupChatClick = onNewGroupChatClick,
                onChatToggled = onChatToggled,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag(CHAT_EXPLORER_LOADING_TAG),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyView(
    data: ChatExplorerUiState.Data,
    selectedChatIds: Set<Long>,
    onNewGroupChatClick: () -> Unit,
    onChatToggled: (chatId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 8.dp),
    ) {
        NewGroupChatItemView(onClick = onNewGroupChatClick)
        data.noteToSelf?.let { item ->
            ChatExplorerItemView(
                item = item,
                isSelected = item.id in selectedChatIds,
                onChatToggled = onChatToggled,
            )
        }
        RecentChatsAndMeetingsHeaderItemView()
        EmptyStateView(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag(CHAT_EXPLORER_EMPTY_TAG),
            imagePainter = painterResource(id = iconPackR.drawable.ic_user_glass),
            title = stringResource(sharedR.string.contacts_empty_title),
        )
    }
}

@Composable
private fun ChatExplorerList(
    data: ChatExplorerUiState.Data,
    selectedChatIds: Set<Long>,
    onNewGroupChatClick: () -> Unit,
    onChatToggled: (chatId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .testTag(CHAT_EXPLORER_LIST_TAG),
        contentPadding = PaddingValues(top = 8.dp),
    ) {
        item(key = "new_group_chat") {
            NewGroupChatItemView(onClick = onNewGroupChatClick)
        }
        data.noteToSelf?.let { item ->
            item(key = "note_to_self:${item.id}") {
                ChatExplorerItemView(
                    item = item,
                    isSelected = item.id in selectedChatIds,
                    onChatToggled = onChatToggled,
                )
            }
        }
        item(key = "header:recent") {
            RecentChatsAndMeetingsHeaderItemView()
        }
        items(items = data.recents, key = { "${it.id}" }) { item ->
            ChatExplorerItemView(
                item = item,
                isSelected = item.id in selectedChatIds,
                onChatToggled = onChatToggled,
            )
        }
        if (data.others.isNotEmpty()) {
            item(key = "header:all") {
                AllContactsChatsAndMeetingsHeaderItemView()
            }
            items(items = data.others, key = { "${it.id}" }) { item ->
                ChatExplorerItemView(
                    item = item,
                    isSelected = item.id in selectedChatIds,
                    onChatToggled = onChatToggled,
                )
            }
        }
    }
}

@Composable
private fun ChatExplorerItemView(
    item: ChatExplorerUiItem,
    isSelected: Boolean,
    onChatToggled: (chatId: Long) -> Unit,
) {
    val resources = LocalResources.current
    val snackbarHostState = LocalSnackBarHostState.current
    val coroutineScope = rememberCoroutineScope()
    ChatExplorerListItemView(
        modifier = Modifier.testTag(CHAT_EXPLORER_ROW_TAG + item.id),
        item = item.withSelected(isSelected),
        onItemClicked = {
            if (item.isEnabled) {
                onChatToggled(item.id)
            } else {
                coroutineScope.launch {
                    snackbarHostState?.showAutoDurationSnackbar(
                        resources.getString(sharedR.string.chat_explorer_read_only_chat_warning)
                    )
                }
            }
        },
    )
}

@Composable
private fun NewGroupChatItemView(onClick: () -> Unit) {
    FlexibleLineListItem(
        modifier = Modifier.testTag(CHAT_EXPLORER_NEW_GROUP_TAG),
        title = stringResource(sharedR.string.general_new_group_chat),
        enableClick = true,
        onClickListener = onClick,
        leadingElement = {
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                MegaIcon(
                    imageVector = IconPack.Medium.Thin.Outline.MessageChatCircle,
                    contentDescription = null,
                    tint = IconColor.Primary,
                    modifier = Modifier.size(24.dp),
                )
            }
        },
        trailingElement = {
            MegaIcon(
                imageVector = IconPack.Medium.Thin.Outline.ChevronRight,
                contentDescription = null,
                tint = IconColor.Secondary,
                modifier = Modifier.size(24.dp),
            )
        },
    )
}

@Composable
internal fun TabsScope.ChatExplorerTab(
    selectionState: ChatExplorerSelectionState,
    onStartNewGroupChat: ((CreateGroupChatNavKey.NewGroupChatResult) -> Unit) -> Unit,
) {
    val viewModel = hiltViewModel<ChatExplorerViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    addTextTabWithScrollableContent(
        tabItem = TabItems(
            title = stringResource(sharedR.string.general_chat),
            testTag = CHAT_TAB_TAG,
        ),
    ) { _, modifier ->
        ChatExplorerContent(
            uiState = uiState,
            selectedChatIds = selectionState.selectedChatIds,
            onNewGroupChatClick = {
                onStartNewGroupChat(viewModel::onContactsSelectedForGroupChat)
            },
            onChatToggled = selectionState::toggleSelection,
            modifier = modifier,
        )
    }
}

@Composable
private fun RecentChatsAndMeetingsHeaderItemView() {
    SectionHeaderItemView(stringResource(sharedR.string.chat_explorer_recent_chats_header))
}

@Composable
private fun AllContactsChatsAndMeetingsHeaderItemView() {
    SectionHeaderItemView(stringResource(sharedR.string.chat_explorer_all_contacts_header))
}

@Composable
private fun SectionHeaderItemView(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MegaText(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            textColor = TextColor.Secondary,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ChatExplorerContentPreview() {
    AndroidThemeForPreviews {
        ChatExplorerContent(
            uiState = ChatExplorerUiState.Data(
                noteToSelf = ChatExplorerUiItem.NoteToSelf(
                    id = 10L,
                    isHint = false,
                    isSelected = false,
                    isEnabled = true,
                ),
                recents = listOf(
                    ChatExplorerUiItem.OneToOneChat(
                        id = 11L,
                        contactName = "Elijah Moore",
                        primaryColor = Color(0xFFE65100),
                        secondaryColor = Color(0xFFFFB74D),
                        userStatus = ChatStatus.Online,
                        isSelected = true,
                        isEnabled = true,
                    ),
                    ChatExplorerUiItem.GroupChat(
                        id = 12L,
                        title = "Design Team",
                        participants = 8,
                        isSelected = false,
                        isEnabled = true,
                    ),
                ),
                others = listOf(
                    ChatExplorerUiItem.Contact(
                        id = 14L,
                        contactName = "Brielle Nguyen",
                        primaryColor = Color(0xFF7CB342),
                        secondaryColor = Color(0xFFC5E1A5),
                        userStatus = ChatStatus.Busy,
                        isSelected = false,
                        isEnabled = true,
                    ),
                ),
            ),
            selectedChatIds = setOf(11L),
            onNewGroupChatClick = {},
            onChatToggled = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ChatExplorerLoadingPreview() {
    AndroidThemeForPreviews {
        ChatExplorerContent(
            uiState = ChatExplorerUiState.Loading,
            selectedChatIds = emptySet(),
            onNewGroupChatClick = {},
            onChatToggled = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ChatExplorerEmptyListPreview() {
    AndroidThemeForPreviews {
        ChatExplorerContent(
            uiState = ChatExplorerUiState.Data(
                noteToSelf = ChatExplorerUiItem.NoteToSelf(
                    id = 10L,
                    isHint = false,
                    isSelected = false,
                    isEnabled = true,
                ),
                recents = emptyList(),
                others = emptyList(),
            ),
            selectedChatIds = emptySet(),
            onNewGroupChatClick = {},
            onChatToggled = {},
        )
    }
}

internal const val CHAT_EXPLORER_TAG = "chat_explorer"
internal const val CHAT_EXPLORER_LIST_TAG = "$CHAT_EXPLORER_TAG:list"
internal const val CHAT_EXPLORER_LOADING_TAG = "$CHAT_EXPLORER_TAG:loading"
internal const val CHAT_EXPLORER_EMPTY_TAG = "$CHAT_EXPLORER_TAG:empty"
internal const val CHAT_EXPLORER_NEW_GROUP_TAG = "$CHAT_EXPLORER_TAG:new_group"
internal const val CHAT_EXPLORER_ROW_TAG = "$CHAT_EXPLORER_TAG:row"
