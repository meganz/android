package mega.privacy.android.feature.photos.presentation.playlists.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.privacy.android.core.nodecomponents.model.NodeSelectionAction
import java.util.Locale

@Composable
fun VideoPlaylistsTabAppBar(
    count: Int,
    isAllSelected: Boolean,
    onSelectAllClicked: () -> Unit,
    onCancelSelectionClicked: () -> Unit,
    removePlaylist: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
}

/**
 * The test tag for Video playlists tab selection top app bar
 */
const val VIDEO_PLAYLISTS_TAB_SELECTION_TOP_APP_BAR_TAG =
    "VIDEO_PLAYLISTS_SELECTION_TOP_APP_BAR_TAG"