package mega.privacy.android.feature.photos.presentation.videos.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaSearchTopAppBar
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.privacy.android.core.nodecomponents.model.NodeSelectionAction
import java.util.Locale

@Composable
fun VideosTabToolbar(
    count: Int,
    isAllSelected: Boolean,
    isSelectionMode: Boolean,
    onSelectAllClicked: () -> Unit,
    onCancelSelectionClicked: () -> Unit,
    searchQuery: String?,
    updateSearchQuery: (String) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "",
    isSearchMode: Boolean = true,
    onBackPressed: () -> Unit = {},
    onSearchingModeChanged: ((Boolean) -> Unit)? = null,
) {
    if (isSelectionMode) {
        MegaTopAppBar(
            modifier = modifier.testTag(VIDEOS_TAB_SELECTION_TOP_APP_BAR_TAG),
            navigationType = AppBarNavigationType.Close(onCancelSelectionClicked),
            title = String.format(Locale.ROOT, "%s", count),
            actions = if (!isAllSelected) {
                listOf(NodeSelectionAction.SelectAll)
            } else {
                emptyList()
            },
            onActionPressed = {
                when (it) {
                    is NodeSelectionAction.SelectAll -> onSelectAllClicked()
                    else -> Unit
                }
            }
        )
    } else {
        MegaSearchTopAppBar(
            modifier = modifier.testTag(VIDEOS_TAB_SEARCH_TOP_APP_BAR_TAG),
            navigationType = AppBarNavigationType.Back(onBackPressed),
            title = title,
            query = searchQuery,
            onQueryChanged = updateSearchQuery,
            isSearchingMode = isSearchMode,
            onSearchingModeChanged = onSearchingModeChanged,
            actions = emptyList()
        )
    }
}

/**
 * The test tag for Videos tab search top app bar
 */
const val VIDEOS_TAB_SEARCH_TOP_APP_BAR_TAG = "VIDEOS_TAB_SEARCH_TOP_APP_BAR_TAG"

/**
 * The test tag for Videos tab selection top app bar
 */
const val VIDEOS_TAB_SELECTION_TOP_APP_BAR_TAG = "VIDEOS_TAB_SELECTION_TOP_APP_BAR_TAG"