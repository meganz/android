package mega.privacy.android.app.mediaplayer.videoplayer.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction
import mega.privacy.android.shared.original.core.ui.controls.appbar.AppBarType
import mega.privacy.android.shared.original.core.ui.controls.appbar.MegaAppBar

@Composable
internal fun LegacyVideoPlayerTopBar(
    title: String,
    menuActions: List<VideoPlayerMenuAction>,
    onBackPressed: () -> Unit,
    onMenuActionClicked: (VideoPlayerMenuAction?) -> Unit,
    modifier: Modifier = Modifier,
) {
    MegaAppBar(
        modifier = modifier.testTag(LEGACY_VIDEO_PLAYER_TOP_BAR_TEST_TAG),
        title = title,
        appBarType = AppBarType.BACK_NAVIGATION,
        onNavigationPressed = onBackPressed,
        actions = menuActions,
        onActionPressed = {
            onMenuActionClicked(it as? VideoPlayerMenuAction)
        },
    )
}

const val LEGACY_VIDEO_PLAYER_TOP_BAR_TEST_TAG = "legacy_video_player_view:top_bar"
