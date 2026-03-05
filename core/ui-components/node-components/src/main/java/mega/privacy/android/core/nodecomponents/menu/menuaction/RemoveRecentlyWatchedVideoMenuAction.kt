package mega.privacy.android.core.nodecomponents.menu.menuaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.icon.pack.IconPack
import javax.inject.Inject
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Remove recently watched video menu action
 */
class RemoveRecentlyWatchedVideoMenuAction @Inject constructor() : MenuActionWithIcon {

    @Composable
    override fun getDescription() =
        stringResource(sharedR.string.video_section_bottom_sheet_option_remove_recently_watched_item)

    @Composable
    override fun getIconPainter() =
        rememberVectorPainter(IconPack.Medium.Thin.Outline.MinusCircle)

    override val orderInCategory: Int
        get() = 95

    override val testTag: String
        get() = "menu_action:remove_recently_watched_video"
}
