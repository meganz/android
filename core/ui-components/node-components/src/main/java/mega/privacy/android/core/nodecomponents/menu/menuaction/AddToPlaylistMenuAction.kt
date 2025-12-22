package mega.privacy.android.core.nodecomponents.menu.menuaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R
import javax.inject.Inject

class AddToPlaylistMenuAction @Inject constructor() : MenuActionWithIcon {
    @Composable
    override fun getIconPainter() = rememberVectorPainter(
        IconPack.Medium.Thin.Outline.Playlist
    )

    @Composable
    override fun getDescription() =
        stringResource(id = R.string.video_section_bottom_sheet_option_title_add_to_playlist)

    override val orderInCategory: Int
        get() = 95
    override val testTag: String
        get() = "menu_action:add_to_playlist"
}