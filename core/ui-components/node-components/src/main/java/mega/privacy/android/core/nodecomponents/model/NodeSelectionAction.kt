package mega.privacy.android.core.nodecomponents.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.android.core.ui.modifiers.infiniteRotation
import mega.privacy.android.core.nodecomponents.menu.menuaction.CopyMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.DownloadMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.HideMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.ManageLinkMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.MoveMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.TrashMenuAction
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR


/**
 * Actions available in the node selection mode.
 */
sealed interface NodeSelectionAction {

    data object SelectAll : MenuActionWithIcon {
        override val testTag: String = "node_selection_action:select_all"

        @Composable
        override fun getDescription() = stringResource(sharedR.string.action_select_all)

        @Composable
        override fun getIconPainter() =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.CheckStack)
    }

    data object Selecting : MenuActionWithIcon {
        override val testTag: String = "node_selection_action:selecting"

        @Composable
        override fun getDescription() =
            stringResource(sharedR.string.app_bar_selection_mode_description)

        @Composable
        override fun getIconPainter() =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.LoaderGrad)

        override val modifier: Modifier
            get() = Modifier.infiniteRotation()
    }

    data object More : MenuActionWithIcon {
        override val testTag: String = "node_selection_action:more"

        @Composable
        override fun getDescription() = stringResource(sharedR.string.general_menu)

        @Composable
        override fun getIconPainter() =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.MoreVertical)
    }

    companion object {
        const val DEFAULT_MAX_VISIBLE_ITEMS = 4
    }
}