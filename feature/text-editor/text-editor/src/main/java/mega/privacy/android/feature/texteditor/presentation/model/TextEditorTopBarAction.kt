package mega.privacy.android.feature.texteditor.presentation.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.feature.texteditor.R
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Top bar actions for the Compose text editor.
 * Other node actions (rename, move, etc.) are available via the Node Options Bottom Sheet (More).
 */
sealed interface TextEditorTopBarAction : MenuActionWithIcon {

    data object Download : TextEditorTopBarAction {
        override val testTag: String = "text_editor_top_bar:download"

        @Composable
        override fun getDescription(): String =
            stringResource(sharedR.string.general_save_to_device)

        @Composable
        override fun getIconPainter(): Painter =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.Download)
    }

    data object LineNumbers : TextEditorTopBarAction {
        override val testTag: String = "text_editor_top_bar:line_numbers"

        @Composable
        override fun getDescription(): String =
            stringResource(sharedR.string.text_editor_show_line_numbers)

        @Composable
        override fun getIconPainter(): Painter =
            painterResource(R.drawable.icon_text_editor_show_line_numbers)
    }

    data object GetLink : TextEditorTopBarAction {
        override val testTag: String = "text_editor_top_bar:get_link"

        @Composable
        override fun getDescription(): String =
            stringResource(sharedR.string.edit_link_option)

        @Composable
        override fun getIconPainter(): Painter =
            rememberVectorPainter(IconPack.Medium.Thin.Solid.Link01)
    }

    data object SendToChat : TextEditorTopBarAction {
        override val testTag: String = "text_editor_top_bar:send_to_chat"

        @Composable
        override fun getDescription(): String =
            stringResource(sharedR.string.context_send_file_to_chat)

        @Composable
        override fun getIconPainter(): Painter =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.MessageArrowUp)
    }

    data object Share : TextEditorTopBarAction {
        override val testTag: String = "text_editor_top_bar:share"

        @Composable
        override fun getDescription(): String =
            stringResource(sharedR.string.general_share)

        @Composable
        override fun getIconPainter(): Painter =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.ShareNetwork)
    }

    data object Save : TextEditorTopBarAction {
        override val testTag: String = "text_editor_top_bar:save"

        @Composable
        override fun getDescription(): String =
            stringResource(sharedR.string.general_action_save)

        @Composable
        override fun getIconPainter(): Painter =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.Check)
    }

    data object More : TextEditorTopBarAction {
        override val testTag: String = "text_editor_top_bar:more"

        @Composable
        override fun getDescription(): String =
            stringResource(sharedR.string.general_menu)

        @Composable
        override fun getIconPainter(): Painter =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.MoreVertical)

        override val orderInCategory: Int
            get() = 999999
    }
}
