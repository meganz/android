package mega.privacy.android.feature.cloudexplorer.presentation.chatexplorer

import android.content.res.Resources
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import de.palm.composestateevents.EventEffect
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.list.FlexibleLineListItem
import mega.android.core.ui.components.state.EmptyStateView
import mega.android.core.ui.components.tabs.TabsScope
import mega.android.core.ui.extensions.showAutoDurationSnackbar
import mega.android.core.ui.model.TabItems
import mega.android.core.ui.preview.BooleanProvider
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.domain.entity.chat.ChatStatus
import mega.privacy.android.feature.cloudexplorer.presentation.explorer.CHAT_TAB_TAG
import mega.privacy.android.feature.cloudexplorer.presentation.search.ChatExplorerSearchContent
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.navigation.destination.CreateGroupChatNavKey
import mega.privacy.android.navigation.destination.ShareTextToMegaNavKey
import mega.privacy.android.shared.chats.components.ChatExplorerListItemView
import mega.privacy.android.shared.chats.model.ChatExplorerUiItem
import mega.privacy.android.shared.resources.R as sharedR

@Composable
internal fun ChatExplorerContent(
    uiState: ChatExplorerUiState,
    isProcessingAction: Boolean,
    selectedChatIds: Set<Long>,
    onNewGroupChatClick: () -> Unit,
    onChatToggled: (chatId: Long) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
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
                items = uiState.items,
                isProcessingAction = isProcessingAction,
                selectedChatIds = selectedChatIds,
                onChatToggled = onChatToggled,
                onNewGroupChatClick = onNewGroupChatClick,
                modifier = modifier,
                listState = listState,
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
        data.items.noteToSelf?.let { item ->
            ChatExplorerItemView(
                item = item,
                isProcessingAction = false,
                isSelected = item.id in selectedChatIds,
                onChatToggled = onChatToggled,
            )
        }
        AllContactsChatsAndMeetingsHeaderItemView()
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
internal fun ChatExplorerList(
    items: ChatExplorerUiState.Items,
    isProcessingAction: Boolean,
    selectedChatIds: Set<Long>,
    onChatToggled: (chatId: Long) -> Unit,
    modifier: Modifier = Modifier,
    onNewGroupChatClick: (() -> Unit)? = null,
    listState: LazyListState = rememberLazyListState(),
) {
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .testTag(CHAT_EXPLORER_LIST_TAG),
        contentPadding = PaddingValues(top = 8.dp),
    ) {
        onNewGroupChatClick?.let { onClick ->
            item(key = "new_group_chat") {
                NewGroupChatItemView(onClick = onClick)
            }
        }
        items.noteToSelf?.let { item ->
            item(key = "note_to_self:${item.id}") {
                ChatExplorerItemView(
                    item = item,
                    isProcessingAction = isProcessingAction,
                    isSelected = item.id in selectedChatIds,
                    onChatToggled = onChatToggled,
                )
            }
        }
        if (items.recents.isNotEmpty()) {
            item(key = "header:recent") {
                RecentChatsAndMeetingsHeaderItemView()
            }
            items(items = items.recents, key = { "${it.id}" }) { item ->
                ChatExplorerItemView(
                    item = item,
                    isProcessingAction = isProcessingAction,
                    isSelected = item.id in selectedChatIds,
                    onChatToggled = onChatToggled,
                )
            }
        }
        if (items.others.isNotEmpty()) {
            item(key = "header:all") {
                AllContactsChatsAndMeetingsHeaderItemView()
            }
            items(items = items.others, key = { "${it.id}" }) { item ->
                ChatExplorerItemView(
                    item = item,
                    isProcessingAction = isProcessingAction,
                    isSelected = item.id in selectedChatIds,
                    onChatToggled = onChatToggled,
                )
            }
        }
    }
}

/**
 * Index of [chatId] within [ChatExplorerList]'s LazyColumn, mirroring its item order, or null if absent.
 */
private fun ChatExplorerUiState.Items.indexOfChat(chatId: Long): Int? = when {
    noteToSelf?.id == chatId -> 1
    else -> {
        val noteToSelfCount = if (noteToSelf == null) 0 else 1

        recents.indexOfFirst { it.id == chatId }.takeIf { it >= 0 }
            ?.let { 2 + noteToSelfCount + it }
            ?: others.indexOfFirst { it.id == chatId }.takeIf { it >= 0 }
                ?.let {
                    val recentsBlock =
                        if (recents.isEmpty()) 0 else 1 + recents.size // header + items

                    2 + noteToSelfCount + recentsBlock + it
                }
    }
}

@Composable
private fun ChatExplorerItemView(
    item: ChatExplorerUiItem,
    isProcessingAction: Boolean,
    isSelected: Boolean,
    onChatToggled: (chatId: Long) -> Unit,
) {
    val resources = LocalResources.current
    val snackbarHostState = LocalSnackBarHostState.current
    val coroutineScope = rememberCoroutineScope()
    ChatExplorerListItemView(
        modifier = Modifier.testTag(CHAT_EXPLORER_ROW_TAG + item.id),
        item = item.withSelected(isSelected),
        isProcessingAction = isProcessingAction,
        onItemClicked = {
            if (!isProcessingAction) {
                if (item.isEnabled) {
                    onChatToggled(item.id)
                } else {
                    coroutineScope.launch {
                        snackbarHostState?.showAutoDurationSnackbar(
                            resources.getString(sharedR.string.chat_explorer_read_only_chat_warning)
                        )
                    }
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
    shareTextToMegaNavKey: ShareTextToMegaNavKey?,
    selectionState: ChatExplorerSelectionState,
    isProcessingAction: Boolean,
    showSearch: Boolean,
    searchQuery: String?,
    onSearchQueryChanged: (String) -> Unit,
    prepareChatsEvent: StateEvent,
    onPrepareChatsConsumed: () -> Unit,
    onChatsReadyToShare: (List<Long>) -> Unit,
    onCloseExplorerScreen: () -> Unit,
    onNavigate: (NavKey) -> Unit,
    monitorResult: (String) -> Flow<Any?>,
    clearResult: (String) -> Unit,
    onHasContentChanged: (Boolean) -> Unit = {},
    onLoadingChanged: (Boolean) -> Unit = {},
    onConnectivityChanged: (Boolean) -> Unit = {},
) {
    val viewModel = hiltViewModel<ChatExplorerViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var selectionBeforeSearch by remember { mutableStateOf(selectionState.selectedChatIds) }
    val resources = LocalResources.current
    val snackbarHostState = LocalSnackBarHostState.current
    val coroutineScope = rememberCoroutineScope()
    val newGroupChatResult by monitorResult(CreateGroupChatNavKey.KEY)
        .collectAsStateWithLifecycle(initialValue = null)
    val onSharedToChats = rememberOnSharedToChats(
        onNavigate = onNavigate,
        onCloseExplorerScreen = onCloseExplorerScreen,
    )

    when (val state = uiState) {
        ChatExplorerUiState.Loading -> LaunchedEffect(Unit) {
            onLoadingChanged(true)
            onHasContentChanged(false)
            onConnectivityChanged(true)
        }

        is ChatExplorerUiState.Data -> {
            LaunchedEffect(Unit) { onLoadingChanged(false) }
            LaunchedEffect(state.isEmpty) { onHasContentChanged(!state.isEmpty) }
            LaunchedEffect(state.isConnected) { onConnectivityChanged(state.isConnected) }
            EventEffect(
                event = state.newChatCreatedEvent,
                onConsumed = viewModel::onNewChatCreatedConsumed,
            ) { chatId ->
                if (chatId !in selectionState.selectedChatIds) {
                    selectionState.toggleSelection(chatId)
                }
                coroutineScope.launch {
                    snackbarHostState?.showAutoDurationSnackbar(
                        resources.getString(sharedR.string.general_new_group_chat_created)
                    )
                }
            }
            EventEffect(
                event = state.chatsReadyToShareEvent,
                onConsumed = viewModel::onChatsReadyToShareConsumed,
            ) { chatIds ->
                if (shareTextToMegaNavKey != null) {
                    onSharedToChats(chatIds)
                } else {
                    onChatsReadyToShare(chatIds)
                }
            }
            LaunchedEffect(showSearch) {
                if (showSearch) {
                    selectionBeforeSearch = selectionState.selectedChatIds
                    return@LaunchedEffect
                }
                val index = (selectionState.selectedChatIds - selectionBeforeSearch)
                    .mapNotNull { state.items.indexOfChat(it) }
                    .maxOrNull() ?: return@LaunchedEffect

                listState.scrollToItem(index)
            }
        }
    }

    LaunchedEffect(newGroupChatResult) {
        (newGroupChatResult as? CreateGroupChatNavKey.NewGroupChatResult)?.let { result ->
            clearResult(CreateGroupChatNavKey.KEY)
            viewModel.onContactsSelectedForGroupChat(result)
        }
    }

    EventEffect(
        event = prepareChatsEvent,
        onConsumed = onPrepareChatsConsumed,
    ) {
        viewModel.prepareChatsForSharing(
            selectedIds = selectionState.selectedChatIds.toList(),
            message = shareTextToMegaNavKey?.buildMessageToShare(resources),
        )
    }

    addTextTabWithScrollableContent(
        tabItem = TabItems(
            title = stringResource(sharedR.string.general_chat),
            testTag = CHAT_TAB_TAG,
        ),
    ) { _, modifier ->
        if (showSearch) {
            ChatExplorerSearchContent(
                query = searchQuery,
                onQueryChanged = onSearchQueryChanged,
                chatExplorerSelectionState = selectionState,
                isProcessingAction = isProcessingAction,
                modifier = modifier,
            )
        } else {
            ChatExplorerContent(
                uiState = uiState,
                isProcessingAction = isProcessingAction,
                selectedChatIds = selectionState.selectedChatIds,
                onNewGroupChatClick = { onNavigate(CreateGroupChatNavKey) },
                onChatToggled = selectionState::toggleSelection,
                modifier = modifier,
                listState = listState,
            )
        }
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

private fun ShareTextToMegaNavKey.buildMessageToShare(resources: Resources): String {
    val emailLine = email?.let {
        "${resources.getString(sharedR.string.new_file_email_when_uploading)}: $it\n\n"
    }.orEmpty()
    return "${subject.orEmpty()}\n\n$emailLine$text"
}

@CombinedThemePreviews
@Composable
private fun ChatExplorerContentPreview(
    @PreviewParameter(BooleanProvider::class) isProcessingAction: Boolean,
) {
    AndroidThemeForPreviews {
        ChatExplorerContent(
            uiState = ChatExplorerUiState.Data(
                items = ChatExplorerUiState.Items(
                    noteToSelf = ChatExplorerUiItem.NoteToSelf(
                        id = 10L,
                        isHint = false,
                        isSelected = false,
                        isEnabled = true,
                        isArchived = false,
                        lastTimestamp = 0L,
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
                            isArchived = false,
                            lastTimestamp = 0L,
                        ),
                        ChatExplorerUiItem.GroupChat(
                            id = 12L,
                            title = "Design Team",
                            participants = 8,
                            isSelected = false,
                            isEnabled = true,
                            isArchived = false,
                            lastTimestamp = 0L,
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
                    )
                ),
                newChatCreatedEvent = consumed(),
                chatsReadyToShareEvent = consumed(),
                searchResults = ChatExplorerUiState.Items.Empty,
                isConnected = true,
            ),
            isProcessingAction = isProcessingAction,
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
            isProcessingAction = false,
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
                items = ChatExplorerUiState.Items(
                    noteToSelf = ChatExplorerUiItem.NoteToSelf(
                        id = 10L,
                        isHint = false,
                        isSelected = false,
                        isEnabled = true,
                        isArchived = false,
                        lastTimestamp = 0L,
                    ),
                    recents = emptyList(),
                    others = emptyList()
                ),
                newChatCreatedEvent = consumed(),
                chatsReadyToShareEvent = consumed(),
                searchResults = ChatExplorerUiState.Items.Empty,
                isConnected = true,
            ),
            isProcessingAction = false,
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
