package mega.privacy.android.app.presentation.videosection.view.playlist

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.AppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import mega.privacy.android.app.presentation.videosection.model.VideoSectionMenuAction
import mega.privacy.android.shared.original.core.ui.controls.appbar.AppBarType
import mega.privacy.android.shared.original.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.shared.original.core.ui.controls.appbar.SelectModeAppBar

@Composable
internal fun VideoPlaylistDetailTopBar(
    title: String,
    isActionMode: Boolean,
    selectedSize: Int,
    onMenuActionClick: (VideoSectionMenuAction?) -> Unit,
    onBackPressed: () -> Unit,
) {
    if (isActionMode) {
        SelectModeAppBar(
            title = selectedSize.toString(),
            actions = if (selectedSize == 0) {
                emptyList()
            } else {
                listOf(
                    VideoSectionMenuAction.VideoSectionRemoveAction,
                    VideoSectionMenuAction.VideoSectionSelectAllAction,
                    VideoSectionMenuAction.VideoSectionClearSelectionAction
                )
            },
            onActionPressed = {
                onMenuActionClick(it as? VideoSectionMenuAction)
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(VIDEO_PLAYLIST_DETAIL_SELECTED_MODE_TOP_BAR_TEST_TAG),
            elevation = AppBarDefaults.TopAppBarElevation,
            onNavigationPressed = onBackPressed
        )
    } else {
        MegaAppBar(
            modifier = Modifier.testTag(VIDEO_PLAYLIST_DETAIL_TOP_BAR_TEST_TAG),
            appBarType = AppBarType.BACK_NAVIGATION,
            title = title,
            onNavigationPressed = onBackPressed,
            actions = listOf(VideoSectionMenuAction.VideoSectionMoreAction),
            onActionPressed = { onMenuActionClick(it as? VideoSectionMenuAction) }
        )
    }
}

/**
 * Test tag for top bar
 */
const val VIDEO_PLAYLIST_DETAIL_TOP_BAR_TEST_TAG = "video_playlist_detail_view:top_bar"

/**
 * Test tag for selected mode top bar
 */
const val VIDEO_PLAYLIST_DETAIL_SELECTED_MODE_TOP_BAR_TEST_TAG =
    "video_playlist_detail_view:top_bar_selected_mode"