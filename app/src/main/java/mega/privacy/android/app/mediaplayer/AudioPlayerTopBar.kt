package mega.privacy.android.app.mediaplayer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import mega.android.core.ui.components.toolbar.TransparentTopBar
import mega.android.core.ui.model.menu.MenuActionString
import mega.privacy.android.app.R
import mega.privacy.android.icon.pack.IconPack

internal data object AudioPlayerMoreActionsMenuAction : MenuActionString(
    icon = IconPack.Medium.Thin.Outline.MoreVertical,
    descriptionRes = R.string.label_more,
    testTag = AUDIO_PLAYER_MORE_ACTIONS_BUTTON_TEST_TAG,
)

@Composable
internal fun AudioPlayerTopBar(
    onBackPressed: () -> Unit,
    onMoreActionsClicked: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "",
    subtitle: String? = null,
) {
    val backIcon: Painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.ChevronDown)
    TransparentTopBar(
        title = title,
        subtitle = subtitle,
        modifier = modifier.testTag(AUDIO_PLAYER_TOP_BAR_TEST_TAG),
        navigationIcon = backIcon,
        onNavigationIconClicked = onBackPressed,
        actions = listOf(AudioPlayerMoreActionsMenuAction),
        onActionPressed = {
            onMoreActionsClicked()
        },
    )
}

/**
 * Test tag for audio player top bar
 */
const val AUDIO_PLAYER_TOP_BAR_TEST_TAG = "audio_player:top_bar"

/**
 * Test tag for the more actions button in the audio player top bar
 */
const val AUDIO_PLAYER_MORE_ACTIONS_BUTTON_TEST_TAG = "audio_player:more_actions_button"
