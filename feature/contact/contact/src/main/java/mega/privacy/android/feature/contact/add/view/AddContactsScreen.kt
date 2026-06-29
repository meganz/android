package mega.privacy.android.feature.contact.add.view

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.android.core.ui.components.fab.MegaFab
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaSearchTopAppBar
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.feature.contact.add.model.AddContactUiState
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.feature.contact.components.ContactListLoadingView
import mega.privacy.android.shared.contact.components.ContactItemView
import mega.privacy.android.shared.contact.model.AvatarData
import mega.privacy.android.shared.contact.model.ContactItemUiState
import mega.privacy.android.shared.resources.R as sharedR
import androidx.compose.ui.graphics.Color

/**
 * Add contacts screen. A MEGA-contacts multi-select picker: search, toggle selection, and
 * confirm to publish the selected contacts. Selection is owned locally via [rememberContactSelectionState]
 * so it survives search/filter changes.
 *
 * @param state
 * @param onSearchQueryChange invoked with the new query text, or null when the search is cleared.
 * @param onConfirm invoked with the handles of the selected contacts.
 * @param onBack invoked when the user navigates back without confirming.
 * @param modifier
 * @param initialSelectedHandles handles to pre-select on first composition.
 * @param titleRes toolbar title shown while nothing is selected; defaults to "Send contacts".
 */
@Composable
internal fun AddContactsScreen(
    state: AddContactUiState,
    onSearchQueryChange: (String?) -> Unit,
    onConfirm: (selectedHandles: Set<Long>) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    initialSelectedHandles: Set<Long> = emptySet(),
    @StringRes titleRes: Int = sharedR.string.send_contacts,
) {
    val selectionState = rememberContactSelectionState(initialSelectedHandles)
    var searchActive by rememberSaveable { mutableStateOf(false) }
    var searchText by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(searchActive) {
        if (!searchActive && searchText.isNotEmpty()) {
            searchText = ""
            onSearchQueryChange(null)
        }
    }

    MegaScaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag(ADD_CONTACTS_SCREEN_TAG),
        topBar = {
            val title = if (selectionState.selectedItemsCount > 0) {
                pluralStringResource(
                    sharedR.plurals.general_selection_num_selected,
                    selectionState.selectedItemsCount,
                    selectionState.selectedItemsCount,
                )
            } else {
                stringResource(titleRes)
            }
            MegaSearchTopAppBar(
                title = title,
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
        floatingActionButton = {
            if (state is AddContactUiState.Data && selectionState.selectedItemsCount > 0) {
                MegaFab(
                    modifier = Modifier.testTag(ADD_CONTACTS_FAB_TAG),
                    onClick = { onConfirm(selectionState.selectedHandles) },
                    painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.SendHorizontal),
                )
            }
        },
    ) { padding ->
        when (state) {
            AddContactUiState.Loading -> ContactListLoadingView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .testTag(ADD_CONTACTS_LOADING_TAG),
            )

            is AddContactUiState.Data -> {
                if (state.isEmpty) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .testTag(ADD_CONTACTS_EMPTY_TAG),
                        contentAlignment = Alignment.Center,
                    ) {
                        MegaText(
                            text = stringResource(sharedR.string.contacts_empty_title),
                            textColor = TextColor.Secondary,
                        )
                    }
                } else {
                    val layoutDirection = LocalLayoutDirection.current
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag(ADD_CONTACTS_LIST_TAG),
                        contentPadding = PaddingValues(
                            start = padding.calculateStartPadding(layoutDirection),
                            top = padding.calculateTopPadding(),
                            end = padding.calculateEndPadding(layoutDirection),
                            bottom = padding.calculateBottomPadding() + FAB_BOTTOM_CLEARANCE,
                        ),
                    ) {
                        items(state.contacts, key = { it.handle }) { contact ->
                            ContactItemView(
                                contactItemUiState = contact,
                                onClick = { selectionState.toggleSelection(contact.handle) },
                                selected = contact.handle in selectionState.selectedHandles,
                                inSelectionMode = true,
                            )
                        }
                    }
                }
            }
        }
    }
}

private val FAB_BOTTOM_CLEARANCE = 88.dp

internal const val ADD_CONTACTS_SCREEN_TAG = "add_contacts_screen"
internal const val ADD_CONTACTS_LOADING_TAG = "add_contacts_screen:loading"
internal const val ADD_CONTACTS_LIST_TAG = "add_contacts_screen:list"
internal const val ADD_CONTACTS_EMPTY_TAG = "add_contacts_screen:empty"
internal const val ADD_CONTACTS_FAB_TAG = "add_contacts_screen:fab"

private class AddContactUiStateProvider : PreviewParameterProvider<AddContactUiState> {
    override val values: Sequence<AddContactUiState> = sequenceOf(
        AddContactUiState.Loading,
        AddContactUiState.Data(contacts = persistentListOf(), query = null),
        AddContactUiState.Data(
            contacts = listOf(
                ContactItemUiState(
                    handle = 1L,
                    displayName = "Alice Anderson",
                    status = ContactItemStatus.Online,
                    lastSeen = null,
                    avatar = AvatarData.Initials(initials = "A", avatarColor = Color(0xFF2E7D32)),
                    isVerified = true,
                    email = "alice@example.com",
                ),
                ContactItemUiState(
                    handle = 2L,
                    displayName = "Bob Brown",
                    status = ContactItemStatus.Away,
                    lastSeen = 45,
                    avatar = AvatarData.Initials(initials = "B", avatarColor = Color(0xFF1565C0)),
                    isVerified = false,
                    email = "bob@example.com",
                ),
            ).toImmutableList(),
            query = null,
        ),
    )
}

@CombinedThemePreviews
@Composable
private fun AddContactsScreenPreview(
    @PreviewParameter(AddContactUiStateProvider::class) state: AddContactUiState,
) {
    AndroidThemeForPreviews {
        AddContactsScreen(
            state = state,
            onSearchQueryChange = {},
            onConfirm = {},
            onBack = {},
        )
    }
}
