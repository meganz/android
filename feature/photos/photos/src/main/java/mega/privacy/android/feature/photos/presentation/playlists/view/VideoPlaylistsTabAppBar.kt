package mega.privacy.android.feature.photos.presentation.playlists.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaSearchTopAppBar
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.privacy.android.core.nodecomponents.model.NodeSelectionAction
import java.util.Locale

@Composable
fun VideoPlaylistsTabAppBar(
    count: Int,
    isAllSelected: Boolean,
    isSelectionMode: Boolean,
    onSelectAllClicked: () -> Unit,
    onCancelSelectionClicked: () -> Unit,
    removePlaylist: () -> Unit,
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
            modifier = modifier.testTag(VIDEO_PLAYLISTS_TAB_SELECTION_TOP_APP_BAR_TAG),
            navigationType = AppBarNavigationType.Close(onCancelSelectionClicked),
            title = String.format(Locale.ROOT, "%s", count),
            actions = buildList {
                add(VideoPlaylistsTrashMenuAction())
                if (!isAllSelected) {
                    add(NodeSelectionAction.SelectAll)
                }
            },
            onActionPressed = {
                when (it) {
                    is NodeSelectionAction.SelectAll -> onSelectAllClicked()
                    is VideoPlaylistsTrashMenuAction -> removePlaylist()
                    else -> Unit
                }
            }
        )
    } else {
        MegaSearchTopAppBar(
            modifier = modifier.testTag(VIDEO_PLAYLISTS_TAB_SEARCH_TOP_APP_BAR_TAG),
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
 * The test tag for Video playlists tab selection top app bar
 */
const val VIDEO_PLAYLISTS_TAB_SELECTION_TOP_APP_BAR_TAG =
    "VIDEO_PLAYLISTS_SELECTION_TOP_APP_BAR_TAG"

/**
 * The test tag for Video playlists tab search top app bar
 */
const val VIDEO_PLAYLISTS_TAB_SEARCH_TOP_APP_BAR_TAG = "VIDEO_PLAYLISTS_TAB_SEARCH_TOP_APP_BAR_TAG"