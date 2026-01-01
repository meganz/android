package mega.privacy.android.feature.photos.presentation.playlists.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R
import javax.inject.Inject


class VideoPlaylistsTrashMenuAction @Inject constructor() : MenuActionWithIcon {
    @Composable
    override fun getIconPainter() =
        rememberVectorPainter(IconPack.Medium.Thin.Outline.Trash)

    @Composable
    override fun getDescription() =
        stringResource(id = R.string.video_section_playlist_bottom_sheet_option_title_delete)

    override val testTag: String = "video_playlist_menu_action:remove_playlists"
}