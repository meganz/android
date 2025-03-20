package mega.privacy.android.app.presentation.videoplayer.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction
import mega.privacy.android.shared.original.core.ui.controls.appbar.AppBarType
import mega.privacy.android.shared.original.core.ui.controls.appbar.MegaAppBar

@Composable
internal fun VideoPlayerTopBar(
    title: String,
    menuActions: List<VideoPlayerMenuAction>,
    onBackPressed: () -> Unit,
    onMenuActionClicked: (VideoPlayerMenuAction?) -> Unit,
    modifier: Modifier = Modifier,
) {
    MegaAppBar(
        modifier = modifier.testTag(VIDEO_PLAYER_TOP_BAR_TEST_TAG),
        title = title,
        appBarType = AppBarType.BACK_NAVIGATION,
        elevation = 0.dp,
        onNavigationPressed = onBackPressed,
        actions = menuActions,
        onActionPressed = {
            onMenuActionClicked(it as? VideoPlayerMenuAction)
        }
    )
}

/**
 * Test tag for video player top bar
 */
const val VIDEO_PLAYER_TOP_BAR_TEST_TAG = "video_player_view:top_bar"