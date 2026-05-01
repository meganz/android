package mega.privacy.android.app.presentation.videoplayer.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import mega.android.core.ui.model.menu.MenuActionString
import mega.privacy.android.app.R
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.original.core.ui.controls.appbar.AppBarType
import mega.privacy.android.shared.original.core.ui.controls.appbar.MegaAppBar

internal data object VideoPlayerMoreActionsMenuAction : MenuActionString(
    icon = IconPack.Medium.Thin.Outline.MoreVertical,
    descriptionRes = R.string.label_more,
    testTag = VIDEO_PLAYER_MORE_ACTIONS_BUTTON_TEST_TAG,
)

@Composable
internal fun VideoPlayerTopBar(
    title: String,
    onBackPressed: () -> Unit,
    onMoreActionsClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MegaAppBar(
        modifier = modifier.testTag(VIDEO_PLAYER_TOP_BAR_TEST_TAG),
        title = title,
        appBarType = AppBarType.BACK_NAVIGATION,
        onNavigationPressed = onBackPressed,
        actions = listOf(VideoPlayerMoreActionsMenuAction),
        onActionPressed = { onMoreActionsClicked() },
    )
}

/**
 * Test tag for video player top bar
 */
const val VIDEO_PLAYER_TOP_BAR_TEST_TAG = "video_player_view:top_bar"

/**
 * Test tag for the more actions button in the video player top bar
 */
const val VIDEO_PLAYER_MORE_ACTIONS_BUTTON_TEST_TAG = "video_player_view:more_actions_button"
