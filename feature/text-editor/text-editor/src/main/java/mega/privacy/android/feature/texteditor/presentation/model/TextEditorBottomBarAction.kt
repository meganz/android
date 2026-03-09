package mega.privacy.android.feature.texteditor.presentation.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Actions for the text editor bottom floating bar.
 * Designer order: Download, Manage Link, Share, Edit. SendToChat is reserved for a later phase.
 */
sealed interface TextEditorBottomBarAction : MenuActionWithIcon {

    data object Download : TextEditorBottomBarAction {
        @Composable
        override fun getDescription() = stringResource(id = sharedR.string.general_save_to_device)

        @Composable
        override fun getIconPainter(): Painter =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.Download)

        override val orderInCategory = 140
        override val testTag = "text_editor_bottom_bar:download"
    }

    data object GetLink : TextEditorBottomBarAction {
        @Composable
        override fun getDescription() = stringResource(id = sharedR.string.edit_link_option)

        @Composable
        override fun getIconPainter(): Painter =
            rememberVectorPainter(IconPack.Medium.Thin.Solid.Link01)

        override val orderInCategory = 160
        override val testTag = "text_editor_bottom_bar:get_link"
    }

    data object Share : TextEditorBottomBarAction {
        @Composable
        override fun getDescription() = stringResource(id = sharedR.string.general_share)

        @Composable
        override fun getIconPainter(): Painter =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.ShareNetwork)

        override val orderInCategory = 190
        override val testTag = "text_editor_bottom_bar:share"
    }

    data object Edit : TextEditorBottomBarAction {
        @Composable
        override fun getDescription() = stringResource(id = sharedR.string.title_edit_profile_info)

        @Composable
        override fun getIconPainter(): Painter =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.Edit)

        override val orderInCategory = 200
        override val testTag = "text_editor_bottom_bar:edit"
    }

    data object SendToChat : TextEditorBottomBarAction {
        @Composable
        override fun getDescription() = stringResource(id = sharedR.string.context_send_file_to_chat)

        @Composable
        override fun getIconPainter(): Painter =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.MessageArrowUp)

        override val orderInCategory = 210
        override val testTag = "text_editor_bottom_bar:send_to_chat"
    }
}
