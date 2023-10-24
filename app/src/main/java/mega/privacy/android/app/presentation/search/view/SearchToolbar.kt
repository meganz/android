package mega.privacy.android.app.presentation.search.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.controls.appbar.ExpandedSearchAppBar
import mega.privacy.android.core.ui.controls.appbar.SelectModeAppBar
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme

/**
 * Search toolbar used in search activity
 */
@Composable
fun SearchToolBar(
    selectionCount: Int,
    searchQuery: String,
    updateSearchQuery: (String) -> Unit,
) {
    if (selectionCount > 0) {
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
private fun PreviewSearchToolbar() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        SearchToolBar(
            selectionCount = 10,
            searchQuery = "searchQuery",
            updateSearchQuery = {},
        )
    }
}