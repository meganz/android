package mega.privacy.android.feature.contact.requests.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.android.core.ui.components.tabs.MegaFixedTabRow
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaSearchTopAppBar
import mega.android.core.ui.model.TabItems
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.contacts.ContactRequestAction
import mega.privacy.android.feature.contact.components.ContactListLoadingView
import mega.privacy.android.feature.contact.requests.model.ContactRequestTab
import mega.privacy.android.feature.contact.requests.model.ContactRequestUiItem
import mega.privacy.android.feature.contact.requests.model.ContactRequestsUiState
import mega.privacy.android.shared.contact.model.AvatarData
import mega.privacy.android.shared.contact.model.ContactItemUiState
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Stateless contact requests screen: a searchable top bar, two tabs (Received / Sent), a list of
 * request rows per tab, empty states, and a per-request actions bottom sheet. All behaviour is
 * hoisted; the screen performs no domain work.
 *
 * @param state UI state to render.
 * @param onSearchQueryChange Invoked with the search text, or null once search is cleared.
 * @param onTabSelected Invoked when the user selects a tab.
 * @param onItemAction Invoked when a bottom-sheet action is picked for a request.
 * @param onBack Invoked when the user navigates back.
 * @param modifier
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactRequestsScreen(
    state: ContactRequestsUiState,
    onSearchQueryChange: (String?) -> Unit,
    onTabSelected: (ContactRequestTab) -> Unit,
    onItemAction: (ContactRequestUiItem, ContactRequestAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchActive by rememberSaveable { mutableStateOf(false) }
    var searchText by rememberSaveable { mutableStateOf("") }
    var selectedRequestForSheet by remember { mutableStateOf<ContactRequestUiItem?>(null) }

    // The saved search text outlives the ViewModel across process death, so hand it back.
    LaunchedEffect(Unit) {
        if (searchActive && searchText.isNotBlank()) onSearchQueryChange(searchText)
    }

    LaunchedEffect(searchActive) {
        if (!searchActive && searchText.isNotEmpty()) {
            searchText = ""
            onSearchQueryChange(null)
        }
    }

    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = modifier
            .fillMaxSize()
            .testTag(CONTACT_REQUESTS_SCREEN_TAG),
        topBar = {
            MegaSearchTopAppBar(
                title = stringResource(sharedR.string.contacts_section_requests),
                navigationType = AppBarNavigationType.Back(onBack),
                query = searchText,
                isSearchingMode = searchActive,
                onQueryChanged = {
                    searchText = it
                    onSearchQueryChange(it.ifBlank { null })
                },
                onSearchingModeChanged = { searchActive = it },
                searchPlaceholder = stringResource(sharedR.string.contacts_search_hint),
            )
        },
    ) { padding ->
        when (state) {
            ContactRequestsUiState.Loading -> ContactListLoadingView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .testTag(CONTACT_REQUESTS_LOADING_TAG),
            )

            is ContactRequestsUiState.Data -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    MegaFixedTabRow(
                        modifier = Modifier.testTag(CONTACT_REQUESTS_TAB_ROW_TAG),
                        tabIndex = state.selectedTab.ordinal,
                        items = listOf(
                            TabItems(title = stringResource(sharedR.string.tab_received_requests)),
                            TabItems(title = stringResource(sharedR.string.tab_sent_requests)),
                        ),
                        onClick = { index ->
                            onTabSelected(ContactRequestTab.entries[index])
                        },
                    )
                    val (tab, requests) = when (state.selectedTab) {
                        ContactRequestTab.Received -> ContactRequestTab.Received to state.received
                        ContactRequestTab.Sent -> ContactRequestTab.Sent to state.sent
                    }
                    ContactRequestsListContent(
                        tab = tab,
                        requests = requests,
                        isSearching = state.query != null,
                        contentPadding = PaddingValues(),
                        onItemClick = { selectedRequestForSheet = it },
                    )
                }
            }
        }
    }

    val selected = selectedRequestForSheet
    if (selected != null) {
        ContactRequestActionsBottomSheet(
            request = selected,
            onDismiss = { selectedRequestForSheet = null },
            onAction = { request, action ->
                selectedRequestForSheet = null
                onItemAction(request, action)
            },
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ContactRequestsScreenPreview(
    @PreviewParameter(ContactRequestsUiStateProvider::class) state: ContactRequestsUiState,
) {
    AndroidThemeForPreviews {
        ContactRequestsScreen(
            state = state,
            onSearchQueryChange = {},
            onTabSelected = {},
            onItemAction = { _, _ -> },
            onBack = {},
        )
    }
}

private class ContactRequestsUiStateProvider :
    PreviewParameterProvider<ContactRequestsUiState> {
    override val values = sequenceOf(
        ContactRequestsUiState.Loading,
        ContactRequestsUiState.Data(
            received = previewRequests(isOutgoing = false),
            sent = previewRequests(isOutgoing = true),
            selectedTab = ContactRequestTab.Received,
            query = null,
        ),
        ContactRequestsUiState.Data(
            received = persistentListOf(),
            sent = persistentListOf(),
            selectedTab = ContactRequestTab.Sent,
            query = null,
        ),
    )
}

private fun previewRequests(isOutgoing: Boolean): ImmutableList<ContactRequestUiItem> =
    persistentListOf(
        previewRequest(1L, "Alice Anderson", "alice@example.com", isOutgoing),
        previewRequest(2L, "Bob Brown", "bob@example.com", isOutgoing),
    )

private fun previewRequest(
    handle: Long,
    displayName: String,
    email: String,
    isOutgoing: Boolean,
) = ContactRequestUiItem(
    handle = handle,
    isOutgoing = isOutgoing,
    contact = ContactItemUiState(
        handle = handle,
        displayName = displayName,
        status = ContactItemStatus.Unknown,
        lastSeen = null,
        avatar = AvatarData.Initials(
            initials = displayName.first().toString(),
            avatarColor = Color(0xFF2E7D32),
        ),
        isVerified = false,
        email = email,
    ),
    createdTime = "2 days ago",
)
