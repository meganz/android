package mega.privacy.android.feature.clouddrive.presentation.rubbishbin.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.model.menu.MenuAction
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as SharedR
import mega.privacy.android.shard.nodes.R as NodesR

sealed interface RubbishBinAppBarAction : MenuAction {
    object Empty : MenuActionWithIcon {
        override val orderInCategory: Int = 95

        @Composable
        override fun getIconPainter() =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.X)

        @Composable
        override fun getDescription() = stringResource(id = SharedR.string.empty_rubbish_bin_menu)

        override val testTag: String = "rubbish_bin_app_bar:empty_bin"
    }

    object Search : MenuActionWithIcon {
        override val orderInCategory: Int = 95

        @Composable
        override fun getIconPainter() =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.SearchLarge)

        @Composable
        override fun getDescription() = stringResource(id = NodesR.string.action_search)

        override val testTag: String = "rubbish_bin_app_bar:search"
    }

    object More : MenuActionWithIcon {
        override val orderInCategory: Int = 100

        @Composable
        override fun getIconPainter() =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.MoreVertical)

        @Composable
        override fun getDescription() = stringResource(id = NodesR.string.label_more)

        override val testTag: String = "rubbish_bin_app_bar:search"
    }
}