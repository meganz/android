package mega.privacy.android.feature.clouddrive.presentation.search.view

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaSearchTopAppBar
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.shared.resources.R as sharedR

@Composable
fun SearchTopAppBar(
    searchText: String,
    onSearchTextChanged: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isExiting by remember { mutableStateOf(false) }
    val localFocusManager = LocalFocusManager.current
    val localKeyboardController = LocalSoftwareKeyboardController.current

    MegaSearchTopAppBar(
        modifier = modifier.padding(end = 8.dp),
        query = searchText,
        title = "",
        navigationType = AppBarNavigationType.Back(onBack),
        searchPlaceholder = stringResource(sharedR.string.search_bar_placeholder_text),
        onQueryChanged = {
            if (!isExiting) {
                onSearchTextChanged(it)
            }
        },
        onSearchAction = {
            localFocusManager.clearFocus()
            localKeyboardController?.hide()
        },
        isSearchingMode = true,
        onSearchingModeChanged = { isSearching ->
            if (!isSearching) {
                isExiting = true
                onBack()
            }
        }
    )
}

@CombinedThemePreviews
@Composable
private fun PreviewSearchTopAppBar() {
    AndroidThemeForPreviews {
        SearchTopAppBar(
            searchText = "Sample Search",
            onSearchTextChanged = {},
            onBack = {}
        )
    }
}