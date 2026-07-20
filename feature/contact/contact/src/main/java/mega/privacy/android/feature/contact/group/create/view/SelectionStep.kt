package mega.privacy.android.feature.contact.group.create.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.android.core.ui.components.fab.MegaFab
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaSearchTopAppBar
import mega.android.core.ui.modifiers.applyScrollToHideFabBehavior
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.feature.contact.components.ContactListLoadingView
import mega.privacy.android.feature.contact.group.create.model.CreateChatUiState
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.contact.components.ContactItemView
import mega.privacy.android.shared.contact.model.AvatarData
import mega.privacy.android.shared.contact.model.ContactItemUiState
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Selection step of a create-chat flow: search and toggle MEGA contacts. Reused by the "create group
 * chat" and "new chat" screens; the [title], FAB [fabIcon] and [tagPrefix] differ per flow.
 *
 * @param state the searchable contacts state.
 * @param selectedHandles the currently selected contact handles.
 * @param selectedCount the number of selected items.
 * @param emptySelectionTitle the top-bar title shown when nothing is selected.
 * @param fabIcon the FAB icon painter.
 * @param tagPrefix the prefix for this step's test tags.
 * @param showFab whether the FAB should be shown for the current selection.
 * @param onSearchQueryChange invoked with the new query text, or null when the search is cleared.
 * @param onToggle invoked with the handle of the contact whose selection is toggled.
 * @param onNext invoked when the FAB is tapped.
 * @param onBack invoked when the user navigates back.
 * @param modifier
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SelectionStep(
    state: CreateChatUiState,
    selectedHandles: Set<Long>,
    selectedCount: Int,
    emptySelectionTitle: String,
    fabIcon: Painter,
    tagPrefix: String,
    showFab: Boolean,
    onSearchQueryChange: (String?) -> Unit,
    onToggle: (Long) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchActive by rememberSaveable { mutableStateOf(false) }
    var searchText by rememberSaveable { mutableStateOf("") }

    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = modifier
            .fillMaxSize()
            .testTag("$tagPrefix$SELECTION_SCREEN_SUFFIX"),
        topBar = {
            val title = if (selectedCount > 0) {
                pluralStringResource(
                    sharedR.plurals.general_selection_num_selected,
                    selectedCount,
                    selectedCount,
                )
            } else {
                emptySelectionTitle
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
                onSearchingModeChanged = {
                    searchActive = it
                    if (!it && searchText.isNotEmpty()) {
                        searchText = ""
                        onSearchQueryChange(null)
                    }
                },
                searchPlaceholder = stringResource(sharedR.string.contacts_search_hint),
            )
        },
        floatingActionButton = {
            if (state is CreateChatUiState.Data && showFab) {
                MegaFab(
                    modifier = Modifier
                        .testTag("$tagPrefix$NEXT_FAB_SUFFIX")
                        .applyScrollToHideFabBehavior(),
                    onClick = {
                        onSearchQueryChange(null)
                        onNext()
                    },
                    painter = fabIcon,
                )
            }
        },
    ) { padding ->
        when (state) {
            CreateChatUiState.Loading -> ContactListLoadingView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .testTag("$tagPrefix$LOADING_SUFFIX"),
            )

            is CreateChatUiState.Data -> {
                if (state.isEmpty) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .testTag("$tagPrefix$EMPTY_SUFFIX"),
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
                            .testTag("$tagPrefix$LIST_SUFFIX"),
                        contentPadding = PaddingValues(
                            start = padding.calculateStartPadding(layoutDirection),
                            top = padding.calculateTopPadding(),
                            end = padding.calculateEndPadding(layoutDirection),
                            bottom = padding.calculateBottomPadding(),
                        ),
                    ) {
                        items(state.contacts, key = { it.handle }) { contact ->
                            ContactItemView(
                                contactItemUiState = contact,
                                onClick = { onToggle(contact.handle) },
                                selected = contact.handle in selectedHandles,
                                inSelectionMode = true,
                            )
                        }
                    }
                }
            }
        }
    }
}

internal const val SELECTION_SCREEN_SUFFIX = "_screen"
internal const val LOADING_SUFFIX = "_screen:loading"
internal const val LIST_SUFFIX = "_screen:list"
internal const val EMPTY_SUFFIX = "_screen:empty"
internal const val NEXT_FAB_SUFFIX = "_screen:next_fab"

private class SelectionStepUiStateProvider : PreviewParameterProvider<CreateChatUiState> {
    override val values: Sequence<CreateChatUiState> = sequenceOf(
        CreateChatUiState.Loading,
        CreateChatUiState.Data(
            contacts = persistentListOf(),
            query = null,
            allowGroupImageSelection = true,
        ),
        CreateChatUiState.Data(
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
            allowGroupImageSelection = true,
        ),
    )
}

@CombinedThemePreviews
@Composable
private fun SelectionStepPreview(
    @PreviewParameter(SelectionStepUiStateProvider::class) state: CreateChatUiState,
) {
    AndroidThemeForPreviews {
        SelectionStep(
            state = state,
            selectedHandles = setOf(1L),
            selectedCount = 1,
            emptySelectionTitle = stringResource(sharedR.string.general_new_group_chat),
            fabIcon = rememberVectorPainter(IconPack.Medium.Thin.Outline.ChevronRight),
            tagPrefix = "preview",
            showFab = true,
            onSearchQueryChange = {},
            onToggle = {},
            onNext = {},
            onBack = {},
        )
    }
}
