package mega.privacy.android.feature.clouddrive.presentation.rubbishbin.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.model.menu.MenuAction
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.R
import mega.privacy.android.icon.pack.IconPack
import javax.inject.Inject

sealed interface RubbishBinAppBarAction : MenuAction {
    class Empty @Inject constructor(
        override val orderInCategory: Int = 95,
    ) : MenuActionWithIcon {
        @Composable
        override fun getIconPainter() =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.X)

        @Composable
        override fun getDescription() = stringResource(id = R.string.context_clear_rubbish)

        override val testTag: String = "rubbish_bin_app_bar:empty_bin"
    }

    class Search @Inject constructor(
        override val orderInCategory: Int = 95,
    ) : MenuActionWithIcon {

        @Composable
        override fun getIconPainter() =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.SearchLarge)

        @Composable
        override fun getDescription() = stringResource(id = R.string.action_search)

        override val testTag: String = "rubbish_bin_app_bar:search"
    }

    class More @Inject constructor(
        override val orderInCategory: Int = 100,
    ) : MenuActionWithIcon {

        @Composable
        override fun getIconPainter() =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.MoreVertical)

        @Composable
        override fun getDescription() = stringResource(id = R.string.label_more)

        override val testTag: String = "rubbish_bin_app_bar:search"
    }
}