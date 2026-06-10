package mega.privacy.android.feature.contact.group.view

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.launch
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.fab.MegaFab
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaSearchTopAppBar
import mega.android.core.ui.extensions.showAutoDurationSnackbar
import mega.android.core.ui.modifiers.applyScrollToHideFabBehavior
import mega.privacy.android.feature.contact.group.model.ContactGroupUiState
import mega.privacy.android.feature.contact.group.model.ContactGroupUiState.Companion.INVALID_GROUP_CHAT_ID
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Stateless contact groups content: the scaffold, search top bar, create-group FAB, the
 * group-chat-created event handling, and the body (loading skeleton, group list, or empty
 * state).
 *
 * Search state is hoisted so this composable can be rendered in any state by [ContactGroupsScreen]
 * and by screenshot tests (e.g. searching mode, or with a triggered error event so the snackbar
 * is captured in context). The group-chat-created [EventEffect] lives here, inside the scaffold
 * body, because it relies on the [LocalSnackBarHostState] that [MegaScaffold] provides only to its
 * own subtree.
 *
 * @param state
 * @param searchActive Whether the search field is shown.
 * @param searchText The current search query text.
 * @param onSearchActiveChange Called when the search field is opened/closed.
 * @param onSearchTextChange Called when the search query text changes.
 * @param onGroupClick Called with the chat id when a group row is tapped.
 * @param onCreateGroupClick Called when the create-group FAB is tapped.
 * @param onGroupChatCreatedConsumed Called once the group-chat-created event has been handled.
 * @param onNavigateToChat Called with the chat id to open after a group chat is created.
 * @param onBackClick Called when the back navigation icon is tapped.
 * @param modifier
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ContactGroupsContent(
    state: ContactGroupUiState,
    searchActive: Boolean,
    searchText: String,
    onSearchActiveChange: (Boolean) -> Unit,
    onSearchTextChange: (String) -> Unit,
    onGroupClick: (chatId: Long) -> Unit,
    onCreateGroupClick: () -> Unit,
    onGroupChatCreatedConsumed: () -> Unit,
    onNavigateToChat: (chatId: Long) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = modifier
            .fillMaxSize()
            .testTag(CONTACT_GROUPS_SCREEN_TAG),
        topBar = {
            MegaSearchTopAppBar(
                navigationType = AppBarNavigationType.Back(onBackClick),
                title = stringResource(sharedR.string.contacts_groups_title),
                query = searchText,
                isSearchingMode = searchActive,
                onQueryChanged = onSearchTextChange,
                onSearchingModeChanged = onSearchActiveChange,
                searchPlaceholder = stringResource(sharedR.string.contacts_groups_search_hint),
            )
        },
        floatingActionButton = {
            if (state is ContactGroupUiState.Data) {
                MegaFab(
                    modifier = Modifier
                        .testTag(CONTACT_GROUPS_FAB_TAG)
                        .applyScrollToHideFabBehavior(),
                    onClick = onCreateGroupClick,
                    painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.Plus),
                )
            }
        },
    ) { padding ->
        val snackbarHostState = LocalSnackBarHostState.current
        val coroutineScope = rememberCoroutineScope()
        val errorMessage = stringResource(sharedR.string.contacts_groups_create_error)

        if (state is ContactGroupUiState.Data) {
            EventEffect(
                event = state.groupChatCreated,
                onConsumed = onGroupChatCreatedConsumed,
            ) { chatId ->
                if (chatId == INVALID_GROUP_CHAT_ID) {
                    coroutineScope.launch {
                        snackbarHostState?.showAutoDurationSnackbar(errorMessage)
                    }
                } else {
                    onNavigateToChat(chatId)
                }
            }
        }

        when (state) {
            ContactGroupUiState.Loading -> ContactGroupsLoadingView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )

            is ContactGroupUiState.Data -> {
                ContactGroupsListContent(
                    state = state,
                    contentPadding = padding,
                    onGroupClick = onGroupClick,
                )
            }
        }
    }
}

internal const val CONTACT_GROUPS_SCREEN_TAG = "contact_groups_screen"
internal const val CONTACT_GROUPS_FAB_TAG = "contact_groups_screen:fab"
