package mega.privacy.android.app.presentation.search.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewParameter
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.controls.appbar.ExpandedSearchAppBar
import mega.privacy.android.core.ui.controls.appbar.SelectModeAppBar
import mega.privacy.android.core.ui.preview.BooleanProvider
import mega.privacy.android.core.ui.preview.CombinedThemePreviews

/**
 * Search toolbar used in search activity
 */
@Composable
fun SearchToolBar(
    selectionMode: Boolean,
    selectionCount: Int,
    searchQuery: String,
    updateSearchQuery: (String) -> Unit,
) {
    if (selectionMode) {
        SelectModeAppBar(title = "$selectionCount")
    } else {
        ExpandedSearchAppBar(
            text = searchQuery,
            hintId = R.string.hint_action_search,
            onSearchTextChange = { updateSearchQuery(it) },
            onCloseClicked = { updateSearchQuery("") },
            elevation = false
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewSearchToolbar(
    @PreviewParameter(BooleanProvider::class) selectionMode: Boolean,
) {
    SearchToolBar(
        selectionMode = selectionMode,
        selectionCount = 10,
        searchQuery = "searchQuery",
        updateSearchQuery = {}
    )
}