package mega.privacy.android.feature.photos.presentation.albums.content.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR

sealed interface AlbumContentSelectionAction : MenuActionWithIcon {
    data object AddItems : AlbumContentSelectionAction {
        override val testTag: String = "album_content_selection_action:add_items"

        @Composable
        override fun getDescription() =
            stringResource(sharedR.string.album_content_action_add_items)

        @Composable
        override fun getIconPainter() =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.Plus)
    }

    data object More : AlbumContentSelectionAction {
        override val testTag: String = "album_content_selection_action:more"

        @Composable
        override fun getDescription() =
            stringResource(sharedR.string.album_content_selection_action_more_description)

        @Composable
        override fun getIconPainter() =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.MoreVertical)
    }

    data object SelectAll : AlbumContentSelectionAction {
        override val testTag: String = "album_content_selection_action:select_all"

        @Composable
        override fun getDescription() = stringResource(sharedR.string.action_select_all)

        @Composable
        override fun getIconPainter() =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.CheckStack)
    }

    data object Rename : AlbumContentSelectionAction {
        override val testTag: String = "album_content_selection_action:rename"

        @Composable
        override fun getDescription() = stringResource(sharedR.string.context_rename)

        @Composable
        override fun getIconPainter() =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.Pen2)
    }

    data object SelectAlbumCover : AlbumContentSelectionAction {
        override val testTag: String = "album_content_selection_action:select_album_cover"

        @Composable
        override fun getDescription() =
            stringResource(sharedR.string.album_content_selection_action_select_album_cover_description)

        @Composable
        override fun getIconPainter() =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.RectangleImageStack)
    }

    data object Share : AlbumContentSelectionAction {
        override val testTag: String = "album_content_selection_action:manage_link"

        @Composable
        override fun getDescription() = pluralStringResource(sharedR.plurals.label_share_links, 1)

        @Composable
        override fun getIconPainter() =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.ShareNetwork)
    }

    data object ManageLink : AlbumContentSelectionAction {
        override val testTag: String = "album_content_selection_action:manage_link"

        @Composable
        override fun getDescription() = stringResource(sharedR.string.edit_link_option)

        @Composable
        override fun getIconPainter() =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.Link01)
    }

    data object RemoveLink : AlbumContentSelectionAction {
        override val testTag: String = "album_content_selection_action:remove_link"

        @Composable
        override fun getDescription() = stringResource(sharedR.string.context_remove_link_menu)

        @Composable
        override fun getIconPainter() =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.LinkOff01)
    }

    data object Hide : AlbumContentSelectionAction {
        override val testTag: String = "album_content_selection_action:hide"

        @Composable
        override fun getDescription() = stringResource(sharedR.string.general_hide_node)

        @Composable
        override fun getIconPainter() =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.EyeOff)
    }

    data object Unhide : AlbumContentSelectionAction {
        override val testTag: String = "album_content_selection_action:unhide"

        @Composable
        override fun getDescription() = stringResource(sharedR.string.general_unhide_node)

        @Composable
        override fun getIconPainter() =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.Eye)
    }

    data object Delete : AlbumContentSelectionAction {
        override val testTag: String = "album_content_selection_action:delete"

        @Composable
        override fun getDescription() =
            stringResource(sharedR.string.album_content_selection_action_delete_description)

        @Composable
        override fun getIconPainter() =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.Trash)

        override val highlightIcon: Boolean = true
    }

    data object RemoveFavourites : AlbumContentSelectionAction {
        override val testTag: String = "album_content_selection_action:remove_favourites"

        @Composable
        override fun getDescription() =
            stringResource(sharedR.string.album_content_action_remove_favourites)

        @Composable
        override fun getIconPainter() =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.Star)

        override val highlightIcon: Boolean = true
    }

    data object Download : AlbumContentSelectionAction {
        override val testTag: String = "offline_selection_action:download"

        @Composable
        override fun getDescription(): String =
            stringResource(sharedR.string.general_save_to_device)

        @Composable
        override fun getIconPainter(): Painter =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.Download)
    }

    data object SendToChat : AlbumContentSelectionAction {
        override val testTag: String = "offline_selection_action:send_to_chat"

        @Composable
        override fun getDescription(): String =
            stringResource(sharedR.string.context_send_file_to_chat)

        @Composable
        override fun getIconPainter(): Painter =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.MessageArrowUp)
    }

    companion object {
        val bottomBarItems = listOf(
            Download,
            SendToChat,
            Share,
            Hide,
            Unhide,
            Delete,
            RemoveFavourites
        )

        val bottomSheetItems = listOf(
            Rename,
            SelectAlbumCover,
            ManageLink,
            RemoveLink,
            Delete
        )
    }
}