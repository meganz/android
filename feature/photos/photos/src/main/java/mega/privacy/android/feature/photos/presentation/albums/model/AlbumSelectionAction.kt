package mega.privacy.android.feature.photos.presentation.albums.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR

sealed interface AlbumSelectionAction : MenuActionWithIcon {
    data object ManageLink : AlbumSelectionAction {
        override val testTag: String = "album_selection_action:manage_link"

        @Composable
        override fun getDescription() = stringResource(sharedR.string.edit_link_option)

        @Composable
        override fun getIconPainter() =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.Link01)
    }

    data object Delete : AlbumSelectionAction {
        override val testTag: String = "album_selection_action:delete"

        @Composable
        override fun getDescription() =
            stringResource(sharedR.string.album_content_selection_action_delete_description)

        @Composable
        override fun getIconPainter() =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.Trash)
    }
}