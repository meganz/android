package mega.privacy.android.feature.photos.presentation.playlists.detail.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR

sealed interface VideoPlaylistDetailSelectionMenuAction : MenuActionWithIcon {
    data object Hide : VideoPlaylistDetailSelectionMenuAction {

        override val testTag: String = "video_playlist_detail_selection_action:hide"

        override val orderInCategory: Int = 110

        @Composable
        override fun getIconPainter() =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.EyeOff)

        @Composable
        override fun getDescription() = stringResource(sharedR.string.general_hide_node)
    }

    data object Unhide : VideoPlaylistDetailSelectionMenuAction {

        override val testTag: String = "video_playlist_detail_selection_action:unhide"

        override val orderInCategory: Int = 110

        @Composable
        override fun getIconPainter() =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.Eye)

        @Composable
        override fun getDescription() = stringResource(id = sharedR.string.general_unhide_node)
    }

    data object RemoveFromPlaylist : VideoPlaylistDetailSelectionMenuAction {

        override val testTag: String = "video_playlist_detail_selection_action:remove_from_playlist"

        override val orderInCategory: Int = 230

        override val highlightIcon: Boolean = true

        @Composable
        override fun getIconPainter() =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.Trash)

        @Composable
        override fun getDescription() =
            stringResource(id = sharedR.string.general_remove)
    }
}