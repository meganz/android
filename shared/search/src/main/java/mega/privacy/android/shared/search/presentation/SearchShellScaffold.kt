package mega.privacy.android.shared.search.presentation

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.sheets.MegaModalBottomSheet
import mega.android.core.ui.components.sheets.MegaModalBottomSheetBackground
import mega.android.core.ui.modifiers.applyScrollToHideBehavior
import mega.android.core.ui.modifiers.excludingBottomPadding
import mega.privacy.android.shared.search.presentation.component.RecentSearchesView
import mega.privacy.android.shared.search.presentation.component.SearchEmptyStateView
import mega.privacy.android.shared.search.presentation.component.SearchFilterBottomSheetContent
import mega.privacy.android.shared.search.presentation.component.SearchFilterChips
import mega.privacy.android.shared.search.presentation.component.SearchTopAppBar
import mega.privacy.android.shared.search.presentation.model.SearchEmptyContent
import mega.privacy.android.shared.search.presentation.model.SearchFilterOptions
import mega.privacy.android.shared.search.presentation.model.SearchShellState

/**
 * Reusable search shell that owns all the chrome shared by every search consumer:
 * the search top bar (with debounce-friendly text callback), the optional filter chips row and
 * its bottom sheet, recent searches, and the landing/loading/empty/results state machine.
 *
 * It carries no node/chat/contact specific type — consumers map their own UI state into
 * [SearchShellState] and plug their result rendering in through [resultsContent]. Selection-mode
 * app bars and bottom bars are provided through [topBarOverride] and [bottomBar].
 *
 * @param state Generic shell state.
 * @param landingContent Content shown before any search, when there are no recent searches.
 * @param emptyContent Content shown when a search returns no results.
 * @param onSearchTextChange Invoked as the user types; the consumer typically debounces and searches.
 * @param onBack Invoked when the user navigates back.
 * @param onRecentSearchSelected Invoked when a recent search is tapped (focus handling is internal).
 * @param onClearRecentSearches Invoked when the user clears recent searches.
 * @param filterOptionsProvider Returns the options to show when a filter chip is tapped, or null.
 * @param onFilterChipClicked Invoked when a filter chip is tapped (e.g. for analytics).
 * @param onFilterOptionSelected Invoked with the filter id and chosen option id (null clears it).
 * @param topBarOverride When non-null, replaces the search top bar (e.g. a selection-mode app bar).
 * @param bottomBar Bottom bar slot (e.g. a selection-mode action bar).
 * @param loadingContent Loading state slot; defaults to a thin progress indicator.
 * @param resultsContent Result list slot; receives the scaffold content padding.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchShellScaffold(
    state: SearchShellState,
    landingContent: SearchEmptyContent,
    emptyContent: SearchEmptyContent,
    onSearchTextChange: (String) -> Unit,
    onBack: () -> Unit,
    onRecentSearchSelected: (String) -> Unit,
    onClearRecentSearches: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    filterOptionsProvider: (filterId: String) -> SearchFilterOptions? = { null },
    onFilterChipClicked: (filterId: String) -> Unit = {},
    onFilterOptionSelected: (filterId: String, optionId: String?) -> Unit = { _, _ -> },
    topBarOverride: (@Composable () -> Unit)? = null,
    bottomBar: @Composable () -> Unit = {},
    loadingContent: @Composable () -> Unit = { DefaultSearchLoading() },
    resultsContent: @Composable (PaddingValues) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val localKeyboardController = LocalSoftwareKeyboardController.current
    val localFocusManager = LocalFocusManager.current
    val filterBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedFilterId by rememberSaveable { mutableStateOf<String?>(null) }

    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = modifier,
        topBar = {
            if (topBarOverride != null) {
                topBarOverride()
            } else {
                SearchTopAppBar(
                    searchText = state.searchText,
                    placeholderText = state.placeholder.text,
                    onSearchTextChanged = onSearchTextChange,
                    onBack = onBack,
                    focusRequester = focusRequester,
                )
            }
        },
        bottomBar = bottomBar,
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .padding(contentPadding.excludingBottomPadding())
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { localFocusManager.clearFocus() })
                }
        ) {
            if (state.filters.isNotEmpty()) {
                SearchFilterChips(
                    modifier = Modifier
                        .applyScrollToHideBehavior()
                        .padding(vertical = 12.dp),
                    filters = state.filters,
                    onFilterClicked = { filterId ->
                        localKeyboardController?.hide()
                        onFilterChipClicked(filterId)
                        selectedFilterId = filterId
                    },
                )
            }

            when {
                state.isPreSearch -> {
                    if (state.recentSearches.isNotEmpty()) {
                        RecentSearchesView(
                            modifier = Modifier
                                .fillMaxSize()
                                .imePadding()
                                .testTag(SEARCH_SHELL_RECENT_SEARCHES_TAG),
                            queries = state.recentSearches,
                            onClicked = { query, openKeyboard ->
                                onRecentSearchSelected(query)
                                coroutineScope.launch {
                                    if (openKeyboard) {
                                        focusRequester.requestFocus()
                                    } else {
                                        localFocusManager.clearFocus()
                                    }
                                }
                            },
                            onClearAllClicked = onClearRecentSearches,
                        )
                    } else if (!state.isRecentSearchesLoading) {
                        SearchEmptyStateView(
                            modifier = Modifier
                                .fillMaxSize()
                                .imePadding()
                                .testTag(SEARCH_SHELL_LANDING_TAG),
                            content = landingContent,
                        )
                    }
                }

                state.isLoading -> loadingContent()

                state.isEmpty -> SearchEmptyStateView(
                    modifier = Modifier.testTag(SEARCH_SHELL_EMPTY_TAG),
                    content = emptyContent,
                )

                else -> Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(SEARCH_SHELL_RESULTS_TAG)
                ) {
                    resultsContent(contentPadding)
                }
            }
        }
    }

    selectedFilterId?.let { filterId ->
        filterOptionsProvider(filterId)?.let { options ->
            MegaModalBottomSheet(
                modifier = Modifier.statusBarsPadding(),
                bottomSheetBackground = MegaModalBottomSheetBackground.Surface1,
                sheetState = filterBottomSheetState,
                onDismissRequest = { selectedFilterId = null },
            ) {
                SearchFilterBottomSheetContent(
                    filterOptions = options,
                    onOptionSelected = { id, optionId ->
                        onFilterOptionSelected(id, optionId)
                        coroutineScope.launch {
                            filterBottomSheetState.hide()
                        }.invokeOnCompletion {
                            selectedFilterId = null
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun DefaultSearchLoading() {
    LinearProgressIndicator(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(SEARCH_SHELL_LOADING_TAG)
    )
}

internal const val SEARCH_SHELL_RECENT_SEARCHES_TAG = "search_shell:recent_searches"
internal const val SEARCH_SHELL_LANDING_TAG = "search_shell:landing"
internal const val SEARCH_SHELL_EMPTY_TAG = "search_shell:empty"
internal const val SEARCH_SHELL_RESULTS_TAG = "search_shell:results"
internal const val SEARCH_SHELL_LOADING_TAG = "search_shell:loading"
