package mega.privacy.android.navigation.contract.menu

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.android.core.ui.modifiers.infiniteRotation
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R

/**
 * Shared menu actions.
 */
sealed interface CommonMenuAction : MenuActionWithIcon {

    data object SelectAll : CommonMenuAction {
        override val testTag: String = "node_selection_action:select_all"

        @Composable
        override fun getDescription() = stringResource(R.string.action_select_all)

        @Composable
        override fun getIconPainter() =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.CheckStack)
    }

    data object Selecting : CommonMenuAction {
        override val testTag: String = "node_selection_action:selecting"

        @Composable
        override fun getDescription() =
            stringResource(R.string.app_bar_selection_mode_description)

        @Composable
        override fun getIconPainter() =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.LoaderGrad)

        override val modifier: Modifier
            get() = Modifier.Companion.infiniteRotation()
    }

    data object More : CommonMenuAction {
        override val testTag: String = "node_selection_action:more"

        @Composable
        override fun getDescription() = stringResource(R.string.general_menu)

        @Composable
        override fun getIconPainter() =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.MoreVertical)

        // To ensure this action is always at the end
        override val orderInCategory: Int
            get() = 999999
    }

    data object Search : CommonMenuAction {
        override val testTag: String = "app_bar:search"

        @Composable
        override fun getDescription() = "Search"

        @Composable
        override fun getIconPainter() =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.SearchSmall)
    }

    companion object {
        const val DEFAULT_MAX_VISIBLE_ITEMS = 4
    }
}