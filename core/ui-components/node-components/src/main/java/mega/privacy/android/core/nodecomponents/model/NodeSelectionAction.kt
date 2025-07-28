package mega.privacy.android.core.nodecomponents.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.model.TopAppBarAction
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR


/**
 * Actions available in the node selection mode.
 */
sealed interface NodeSelectionAction : TopAppBarAction {

    data object SelectAll : TopAppBarAction {
        override val testTag: String = "node_selection_action:select_all"

        @Composable
        override fun getDescription() = "Select all" // TODO: Use string resource

        @Composable
        override fun getIconPainter() =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.CheckStack)
    }

    data object Download : NodeSelectionActionString(
        testTag = "node_selection_action:download",
        descriptionRes = sharedR.string.general_save_to_device,
        imageVector = IconPack.Medium.Thin.Outline.Download
    )

    data class ShareLink(
        private val count: Int,
    ) : TopAppBarAction {
        override val testTag: String = "node_selection_action:share_link"

        @Composable
        override fun getDescription() = pluralStringResource(
            id = sharedR.plurals.label_share_links,
            count = count
        )

        @Composable
        override fun getIconPainter() = rememberVectorPainter(IconPack.Medium.Thin.Outline.Link01)
    }

    data object Move : NodeSelectionActionString(
        testTag = "node_selection_action:move",
        descriptionRes = sharedR.string.general_move,
        imageVector = IconPack.Medium.Thin.Outline.Move
    )

    data object RubbishBin : NodeSelectionActionString(
        testTag = "node_selection_action:rubbish_bin",
        descriptionRes = sharedR.string.general_section_rubbish_bin,
        imageVector = IconPack.Medium.Thin.Outline.Trash
    )

    data object More : NodeSelectionActionString(
        testTag = "node_selection_action:more",
        descriptionRes = sharedR.string.general_menu,
        imageVector = IconPack.Medium.Thin.Outline.MoreVertical
    )

    /**
     * Helper class to build node selection TopAppBarAction
     * @property descriptionRes
     */
    abstract class NodeSelectionActionString(
        @StringRes val descriptionRes: Int,
        val imageVector: ImageVector,
        override val testTag: String,
    ) : TopAppBarAction {
        @Composable
        override fun getDescription() = stringResource(id = descriptionRes)

        @Composable
        override fun getIconPainter() = rememberVectorPainter(imageVector)
    }
}