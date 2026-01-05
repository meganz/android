package mega.privacy.android.feature.photos.presentation.playlists.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import jakarta.inject.Inject
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R

class VideoPlaylistRenameMenuAction @Inject constructor() : MenuActionWithIcon {

    @Composable
    override fun getIconPainter() =
        rememberVectorPainter(IconPack.Medium.Thin.Outline.Pen2)

    @Composable
    override fun getDescription() =
        stringResource(id = R.string.context_rename)

    override val testTag: String = "video_playlist_menu_action:rename_playlist"
}