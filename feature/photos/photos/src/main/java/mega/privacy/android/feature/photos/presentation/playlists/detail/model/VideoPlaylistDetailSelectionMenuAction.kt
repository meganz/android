package mega.privacy.android.feature.photos.presentation.playlists.detail.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR

sealed interface VideoPlaylistDetailSelectionMenuAction : MenuActionWithIcon {
    /**
     * Download menu action
     */
    data object Download : VideoPlaylistDetailSelectionMenuAction {

        @Composable
        override fun getDescription() = stringResource(id = sharedR.string.general_save_to_device)

        @Composable
        override fun getIconPainter() = rememberVectorPainter(IconPack.Medium.Thin.Outline.Download)

        override val orderInCategory = 100

        override val testTag: String = "video_playlist_detail_selection:download"
    }

    data object SendToChat : VideoPlaylistDetailSelectionMenuAction {

        @Composable
        override fun getDescription() =
            stringResource(id = sharedR.string.context_send_file_to_chat)

        @Composable
        override fun getIconPainter() =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.MessageArrowUp)

        override val orderInCategory = 110

        override val testTag: String = "video_playlist_detail_selection:send_to_chat"
    }

    data object Share : VideoPlaylistDetailSelectionMenuAction {

        @Composable
        override fun getDescription() = stringResource(id = sharedR.string.general_share)

        @Composable
        override fun getIconPainter() =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.ShareNetwork)

        override val orderInCategory = 120

        override val testTag: String = "video_playlist_detail_selection:share"
    }

    data object RemoveFavourite : VideoPlaylistDetailSelectionMenuAction {
        @Composable
        override fun getIconPainter() =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.HeartBroken)

        @Composable
        override fun getDescription() =
            stringResource(id = sharedR.string.file_properties_unfavourite)

        override val orderInCategory = 130
        override val testTag: String = "video_playlist_detail_selection:remove_favourite"
    }

    data object Hide : VideoPlaylistDetailSelectionMenuAction {
        @Composable
        override fun getIconPainter() =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.EyeOff)

        @Composable
        override fun getDescription() = stringResource(sharedR.string.general_hide_node)

        override val orderInCategory: Int = 140

        override val testTag: String = "video_playlist_detail_selection_action:hide"
    }

    data object Unhide : VideoPlaylistDetailSelectionMenuAction {
        @Composable
        override fun getIconPainter() =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.Eye)

        @Composable
        override fun getDescription() = stringResource(id = sharedR.string.general_unhide_node)

        override val orderInCategory: Int = 140

        override val testTag: String = "video_playlist_detail_selection_action:unhide"
    }

    data object RemoveFromPlaylist : VideoPlaylistDetailSelectionMenuAction {
        @Composable
        override fun getIconPainter() =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.Trash)

        @Composable
        override fun getDescription() =
            stringResource(id = sharedR.string.general_remove)

        override val orderInCategory: Int = 150

        override val highlightIcon: Boolean = true

        override val testTag: String = "video_playlist_detail_selection_action:remove_from_playlist"
    }

    data object SortOrder : VideoPlaylistDetailSelectionMenuAction {
        @Composable
        override fun getIconPainter() =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.SlidersVertical02)

        @Composable
        override fun getDescription() =
            stringResource(id = sharedR.string.action_sort_by_header)

        override val orderInCategory: Int = 160

        override val testTag: String = "video_playlist_detail_selection_action:sort_order"
    }
}
