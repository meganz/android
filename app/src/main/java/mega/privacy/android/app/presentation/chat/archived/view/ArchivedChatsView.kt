package mega.privacy.android.app.presentation.chat.archived.view

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.changepassword.view.Constants
import mega.privacy.android.app.presentation.chat.archived.model.ArchivedChatsState
import mega.privacy.android.app.presentation.chat.dialog.view.ChatRoomItemBottomSheetView
import mega.privacy.android.app.presentation.chat.list.view.ChatListView
import mega.privacy.android.core.ui.controls.appbar.SearchAppBar
import mega.privacy.android.core.ui.model.SearchWidgetState
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.extensions.black_white
import mega.privacy.android.domain.entity.chat.ChatRoomItem

/**
 * Archived chats view
 *
 * @param state                 [ArchivedChatsState]
 * @param onItemClick
 * @param onItemUnarchived
 * @param onBackPressed
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ArchivedChatsView(
    state: ArchivedChatsState,
    onItemClick: (Long) -> Unit = {},
    onItemUnarchived: (Long) -> Unit = {},
    onBackPressed: () -> Unit = {},
    onSnackBarDismiss: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberScaffoldState()
    val sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    var sheetItem by remember { mutableStateOf<ChatRoomItem?>(null) }
    var filteredChats by remember { mutableStateOf<List<ChatRoomItem>?>(listOf()) }
    var searchState by remember { mutableStateOf(SearchWidgetState.COLLAPSED) }
    var searchQuery by remember { mutableStateOf("") }
    var showElevation by remember { mutableStateOf(false) }
    val hideSheet = { scope.launch { sheetState.hide() } }

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetContent = {
            val item = sheetItem
            if (item != null) {
                ChatRoomItemBottomSheetView(
                    item = item,
                    onUnarchiveClick = { onItemUnarchived(item.chatId).also { hideSheet() } }
                )
            } else {
                hideSheet()
            }
        }
    ) {
        Scaffold(
            scaffoldState = scaffoldState,
            snackbarHost = { snackBarHostState ->
                SnackbarHost(hostState = snackBarHostState) { data ->
                    Snackbar(
                        modifier = Modifier.testTag(Constants.SNACKBAR_TEST_TAG),
                        snackbarData = data,
                        backgroundColor = MaterialTheme.colors.black_white
                    )
                }
            },
            topBar = {
                SearchAppBar(
                    searchWidgetState = searchState,
                    typedSearch = searchQuery,
                    onSearchTextChange = { searchQuery = it },
                    onCloseClicked = { searchState = SearchWidgetState.COLLAPSED },
                    onSearchClicked = { searchState = SearchWidgetState.EXPANDED },
                    onBackPressed = onBackPressed,
                    elevation = showElevation,
                    titleId = R.string.archived_chat,
                    hintId = R.string.hint_action_search
                )
            }
        ) { padding ->
            ChatListView(
                modifier = Modifier.padding(padding),
                items = filteredChats ?: state.items,
                selectedIds = emptyList(),
                scrollToTop = false,
                isMeetingView = false,
                onItemClick = onItemClick,
                onItemMoreClick = { chatItem ->
                    sheetItem = chatItem
                    scope.launch { sheetState.show() }
                },
                onFirstItemVisible = { showElevation = !it }
            )
        }
    }

    LaunchedEffect(searchQuery) {
        searchQuery.takeIf(String::isNotBlank)?.let { searchQuery ->
            filteredChats = state.items.filter { item ->
                item.title.contains(searchQuery, true) ||
                        item.lastMessage?.contains(searchQuery, true) == true
            }
        } ?: run {
            filteredChats = null
        }
    }

    LaunchedEffect(state.snackBar) {
        state.snackBar?.let { snackBar ->
            scaffoldState.snackbarHostState.showSnackbar(
                message = snackBar.getMessage(context.resources)
            )
            onSnackBarDismiss()
        }
    }

    BackHandler(sheetState.isVisible) { hideSheet() }
}

@CombinedThemePreviews
@Composable
private fun PreviewEmptyView() {
    ArchivedChatsView(
        state = ArchivedChatsState(),
    )
}
