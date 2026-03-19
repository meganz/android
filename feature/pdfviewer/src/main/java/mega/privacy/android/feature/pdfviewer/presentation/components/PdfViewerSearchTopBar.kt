package mega.privacy.android.feature.pdfviewer.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaSearchTopAppBar
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Search mode top bar for PDF Viewer.
 *
 * Wraps [MegaSearchTopAppBar] with PDF-specific behavior:
 * - Back arrow (←) closes search mode entirely
 * - Search field auto-focuses on appearance
 * - X clears the query string (does NOT close search)
 *
 *
 * @param query The current search query
 * @param onQueryChanged Callback when the query changes
 * @param onClose Callback when search is closed
 * @param modifier Modifier for the composable
 * @param focusRequester Focus requester for the search field
 */
@Composable
internal fun PdfViewerSearchTopBar(
    onQueryChanged: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    query: String? = null,
    isSearchingMode: Boolean = true,
    title: String = "",
    focusRequester: FocusRequester = remember { FocusRequester() },
) {
    var isExiting by remember { mutableStateOf(false) }
    val localFocusManager = LocalFocusManager.current
    val localKeyboardController = LocalSoftwareKeyboardController.current
    val localResources = LocalResources.current

    MegaSearchTopAppBar(
        modifier = modifier,
        query = query,
        title = title,
        navigationType = AppBarNavigationType.Back(onClose),
        searchPlaceholder = localResources.getString(sharedR.string.pdf_viewer_search_placeholder), // TODO: Use string resource
        onQueryChanged = {
            if (!isExiting) onQueryChanged(it)
        },
        onSearchAction = {
            localFocusManager.clearFocus()
            localKeyboardController?.hide()
        },
        isSearchingMode = isSearchingMode,
        onSearchingModeChanged = { isSearching ->
            if (!isSearching) {
                isExiting = true
                onClose()
            }
        },
        focusRequester = focusRequester,
    )
}

@CombinedThemePreviews
@Composable
private fun PreviewPdfViewerSearchTopBar() {
    AndroidThemeForPreviews {
        PdfViewerSearchTopBar(
            onQueryChanged = {},
            onClose = {},
            isSearchingMode = false
        )
    }
}
