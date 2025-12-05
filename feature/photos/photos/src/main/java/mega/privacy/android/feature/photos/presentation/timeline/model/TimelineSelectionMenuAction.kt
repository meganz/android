package mega.privacy.android.feature.photos.presentation.timeline.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as SharedR

sealed interface TimelineSelectionMenuAction : MenuActionWithIcon {

    data object Download : TimelineSelectionMenuAction {

        override val testTag: String = "timeline_selection_action:download"

        override val orderInCategory: Int = 120

        @Composable
        override fun getIconPainter(): Painter =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.Download)

        @Composable
        override fun getDescription(): String =
            stringResource(SharedR.string.general_save_to_device)
    }

    data object ShareLink : TimelineSelectionMenuAction {

        override val testTag: String = "timeline_selection_action:share_link"

        override val orderInCategory: Int = 160

        @Composable
        override fun getIconPainter() =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.Link01)

        @Composable
        override fun getDescription() = pluralStringResource(SharedR.plurals.label_share_links, 1)
    }

    data object SendToChat : TimelineSelectionMenuAction {

        override val testTag: String = "timeline_selection_action:send_to_chat"

        override val orderInCategory: Int = 171

        @Composable
        override fun getIconPainter(): Painter =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.MessageArrowUp)

        @Composable
        override fun getDescription(): String =
            stringResource(SharedR.string.context_send_file_to_chat)
    }

    data object Share : TimelineSelectionMenuAction {

        override val testTag: String = "timeline_selection_action:share"

        override val orderInCategory: Int = 190

        @Composable
        override fun getIconPainter() =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.ShareNetwork)

        @Composable
        override fun getDescription() = stringResource(id = SharedR.string.general_share)
    }

    data object RemoveLink : TimelineSelectionMenuAction {

        override val testTag: String = "timeline_selection_action:remove_link"

        override val orderInCategory: Int = 110

        @Composable
        override fun getIconPainter() =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.LinkOff01)

        @Composable
        override fun getDescription() = stringResource(SharedR.string.context_remove_link_menu)
    }

    data object Hide : TimelineSelectionMenuAction {

        override val testTag: String = "timeline_selection_action:hide"

        override val orderInCategory: Int = 110

        @Composable
        override fun getIconPainter() =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.EyeOff)

        @Composable
        override fun getDescription() = stringResource(SharedR.string.general_hide_node)
    }

    data object Unhide : TimelineSelectionMenuAction {

        override val testTag: String = "timeline_selection_action:unhide"

        override val orderInCategory: Int = 110

        @Composable
        override fun getIconPainter() =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.Eye)

        @Composable
        override fun getDescription() = stringResource(id = SharedR.string.general_unhide_node)
    }

    data object Move : TimelineSelectionMenuAction {

        override val testTag: String = "timeline_selection_action:move"

        override val orderInCategory: Int = 200

        @Composable
        override fun getIconPainter() = rememberVectorPainter(IconPack.Medium.Thin.Outline.Move)

        @Composable
        override fun getDescription() = stringResource(id = SharedR.string.general_move)
    }

    data object Copy : TimelineSelectionMenuAction {

        override val testTag: String = "timeline_selection_action:copy"

        override val orderInCategory: Int = 210

        @Composable
        override fun getIconPainter() = rememberVectorPainter(IconPack.Medium.Thin.Outline.Copy01)

        @Composable
        override fun getDescription() = stringResource(id = SharedR.string.general_copy)
    }

    data object AddToAlbum : TimelineSelectionMenuAction {

        override val testTag: String = "timeline_selection_action:add_to_album"

        override val orderInCategory: Int = 215

        @Composable
        override fun getIconPainter() =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.RectangleStackPlus)

        @Composable
        override fun getDescription() = stringResource(id = SharedR.string.album_add_to_image)
    }

    data object MoveToRubbishBin : TimelineSelectionMenuAction {

        override val testTag: String = "timeline_selection_action:move_to_rubbish_bin"

        override val orderInCategory: Int = 230

        override val highlightIcon: Boolean = true

        @Composable
        override fun getIconPainter() =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.Trash)

        @Composable
        override fun getDescription() =
            stringResource(id = SharedR.string.node_option_move_to_rubbish_bin)
    }

    data object More : TimelineSelectionMenuAction {

        override val testTag: String = "timeline_selection_action:more"

        override val orderInCategory: Int = 231

        @Composable
        override fun getDescription() =
            stringResource(SharedR.string.album_content_selection_action_more_description)

        @Composable
        override fun getIconPainter() =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.MoreVertical)
    }
}
