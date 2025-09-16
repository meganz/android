package mega.privacy.android.core.nodecomponents.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.model.TopAppBarAction
import mega.android.core.ui.modifiers.infiniteRotation
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR


/**
 * Actions available in the node selection mode.
 */
sealed interface NodeSelectionAction : TopAppBarAction {

    data object SelectAll : TopAppBarAction {
        override val testTag: String = "node_selection_action:select_all"

        @Composable
        override fun getDescription() = stringResource(sharedR.string.action_select_all)

        @Composable
        override fun getIconPainter() =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.CheckStack)
    }

    data object Selecting : TopAppBarAction {
        override val testTag: String = "node_selection_action:selecting"

        @Composable
        override fun getDescription() = stringResource(sharedR.string.general_selecting)

        @Composable
        override fun getIconPainter() =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.LoaderGrad)

        override val modifier: Modifier
            get() = Modifier.infiniteRotation()
    }

    data object Download : NodeSelectionActionString(
        testTag = "node_selection_action:download",
        descriptionRes = sharedR.string.general_save_to_device,
        imageVector = IconPack.Medium.Thin.Outline.Download
    )

    data class ShareLink(
        private val count: Int,
    ) : NodeSelectionAction {
        override val testTag: String = "node_selection_action:share_link"

        @Composable
        override fun getDescription() = pluralStringResource(
            id = sharedR.plurals.label_share_links,
            count = count
        )

        @Composable
        override fun getIconPainter() = rememberVectorPainter(IconPack.Medium.Thin.Outline.Link01)
    }

    data object Copy : NodeSelectionActionString(
        testTag = "node_selection_action:copy",
        descriptionRes = sharedR.string.general_copy,
        imageVector = IconPack.Medium.Thin.Outline.Copy01
    )

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

    data object Hide : NodeSelectionActionString(
        testTag = "node_selection_action:hide",
        descriptionRes = sharedR.string.general_hide_node,
        imageVector = IconPack.Medium.Thin.Outline.EyeOff
    )

    data object DeletePermanently : NodeSelectionActionString(
        testTag = "node_selection_action:remove",
        descriptionRes = sharedR.string.rubbish_bin_bottom_menu_option_delete,
        imageVector = IconPack.Medium.Thin.Outline.X
    )

    data object Restore : NodeSelectionActionString(
        testTag = "node_selection_action:restore",
        descriptionRes = sharedR.string.context_restore,
        imageVector = IconPack.Medium.Thin.Outline.RotateCcw
    )

    /**
     * Helper class to build node selection TopAppBarAction
     * @property descriptionRes
     */
    abstract class NodeSelectionActionString(
        @StringRes val descriptionRes: Int,
        val imageVector: ImageVector,
        override val testTag: String,
    ) : NodeSelectionAction {
        @Composable
        override fun getDescription() = stringResource(id = descriptionRes)

        @Composable
        override fun getIconPainter() = rememberVectorPainter(imageVector)
    }

    companion object {
        const val DEFAULT_MAX_VISIBLE_ITEMS = 4

        val defaults = getDefaults(1)

        fun getDefaults(size: Int) = listOf(
            Download,
            ShareLink(size),
            Hide,
            Move,
            Copy,
            RubbishBin
        )
    }
}