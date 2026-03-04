package mega.privacy.android.feature.photos.presentation.videos.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R
import javax.inject.Inject

class VideoRecentlyWatchedClearMenuAction @Inject constructor() : MenuActionWithIcon {
    @Composable
    override fun getIconPainter() =
        rememberVectorPainter(IconPack.Medium.Thin.Outline.Trash)

    @Composable
    override fun getDescription() =
        stringResource(id = R.string.general_clear)

    override val testTag: String = "video_recently_watched_menu_action:clear"
}